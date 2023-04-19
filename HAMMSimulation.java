/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation
 *               of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009, The University of Melbourne, Australia
 */



import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.text.DecimalFormat;
import java.util.*;

/**
 * A simple example showing how to create
 * a datacenter with two hosts and run two
 * cloudlets on it. The cloudlets run in
 * VMs with different MIPS requirements.
 * The cloudlets will take different time
 * to complete the execution depending on
 * the requested VM performance.
 */
public class HAMMSimulation {

    /** The cloudlet list. */
    private static List<Cloudlet> cloudletList;

    /** The vmlist. */
    private static List<Vm> vmlist;

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {

        Log.printLine("Starting CloudSimExample...");

        try {
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1;   // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            // Second step: Create Datacenters
            //Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
            @SuppressWarnings("unused")
            Datacenter datacenter0 = createDatacenter("Datacenter_0");

            //Third step: Create Broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            //Fourth step: Create one virtual machine
            vmlist = new ArrayList<Vm>();

            //VM description
            int vmCount = 6;
            int mips = 100;
            long size = 10000; //image size (MB)
            int ram = 500; //vm memory (MB)
            long bw = 1000;
            int pesNumber = 1; //number of cpus
            String vmm = "Xen"; //VMM name

            //create VMs and add to our VM list
            for(int i = 1; i <= vmCount; i++) {
                Vm vm = new Vm(i, brokerId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared());
                vmlist.add(vm);
            }

            //submit vm list to the broker
            broker.submitVmList(vmlist);


            //Fifth step: Create two Cloudlets
            cloudletList = new ArrayList<Cloudlet>();

            //Cloudlet properties
            long fileSize = 300;
            long outputSize = 300;
            UtilizationModel utilizationModel = new UtilizationModelFull();

            // generate task sizes
            List<Integer> tasks = new ArrayList<>();
            int numTasks = 100;
            Random rand = new Random(5555);
            for(int i = 0; i < numTasks; i++){
                tasks.add(rand.nextInt(100000));
            }

            //Next, we will do the HAMM algorithm to decide which tasks should be bound to which VM
            List<List<Integer>> vms = HAMM(tasks, vmCount);
            System.out.println(vms);

            //Next, we will bind our cloudlets to the VMs, and run the simulation
            int id = 0;
            List<List<Cloudlet>> tasksInVMs = new ArrayList<>();
            for(int i = 0; i < vms.size(); i++){
                List<Integer> vmList = vms.get(i);
                List<Cloudlet> cloudlets = new ArrayList<>();
                for(int j = 0; j < vmList.size(); j++){
                    Integer taskSize = vmList.get(j);
                    Cloudlet cloudlet = new Cloudlet(id++, taskSize, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
                    cloudlet.setUserId(brokerId);
                    cloudletList.add(cloudlet);
                    cloudlets.add(cloudlet);
                    //broker.bindCloudletToVm(cloudlet.getCloudletId(), vmList.get(i));
                }
                tasksInVMs.add(cloudlets);
            }
            broker.submitCloudletList(cloudletList);

            for(int i = 0; i < tasksInVMs.size(); i++){
                List<Cloudlet> cloudlets = tasksInVMs.get(i);
                for(int j = 0; j < cloudlets.size(); j++){
                    // BIND VM TO CLOUDLET
                    Cloudlet cloudlet = cloudlets.get(j);
                    broker.bindCloudletToVm(cloudlet.getCloudletId(), vmlist.get(i).getId());
                }
            }

            CloudSim.startSimulation();


            // Final step: Print results when simulation is over
            List<Cloudlet> newList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList(newList);

            System.out.println("Load Variance: " + calculateLoadBalance(vms));

            Log.printLine("HAMM Simulation finished!");

        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    public static Datacenter createDatacenter(String name){

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store
        //    our machine
        List<Host> hostList = new ArrayList<Host>();

        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 1000;

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating

        //4. Create Hosts with its id and list of PEs and add them to the list of machines
        int hostId=0;
        int ram = 2048; //host memory (MB)
        long storage = 1000000; //host storage
        int bw = 10000;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList,
                        new VmSchedulerTimeShared(peList)
                )
        ); // This is our first machine

        //create another machine in the Data center
        List<Pe> peList2 = new ArrayList<Pe>();

        peList2.add(new Pe(0, new PeProvisionerSimple(mips)));

        hostId++;

        hostList.add(
                new Host(
                        hostId,
                        new RamProvisionerSimple(ram),
                        new BwProvisionerSimple(bw),
                        storage,
                        peList2,
                        new VmSchedulerTimeShared(peList2)
                )
        ); // This is our second machine



        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        double costPerStorage = 0.001;	// the cost of using storage in this resource
        double costPerBw = 0.0;			// the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;
        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    //We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
    //to the specific rules of the simulated scenario
    public static DatacenterBroker createBroker(){

        DatacenterBroker broker = null;
        try {
            broker = new DatacenterBroker("Broker");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    /**
     * Prints the Cloudlet objects
     * @param list  list of Cloudlets
     */
    public static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
                Log.print("SUCCESS");

                Log.printLine( indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
                        indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }

    }

    public static List<List<Integer>> HAMM(List<Integer> taskSizes, int vmCount) {
        List<List<Integer>> vms = new ArrayList<>();
        for(int i = 0; i < vmCount; i++){
            vms.add(new ArrayList<>());
        }
        //find how many tasks are above average vs below average
        while (!taskSizes.isEmpty()) {
            Double average = taskSizes.stream().mapToInt(val -> val).average().orElse(0.0);
            int lower = 0;
            int higher = 0;
            for (Integer size : taskSizes) {
                if (size <= average) {
                    lower++;
                } else {
                    higher++;
                }
            }
            if (lower >= higher) {
                maxMin(taskSizes, vms);
            } else {
                minMin(taskSizes, vms);
            }
        }
        return vms;
    }

    public static void maxMin(List<Integer> tasks, List<List<Integer>> vms){
        // take max from tasks, assign to min vm
        Integer val = Collections.max(tasks);
        addToMinVM(val, vms);
        tasks.remove(val);
    }

    public static void minMin(List<Integer> tasks, List<List<Integer>> vms){
        // take min from tasks, assign to min vm
        Integer val = Collections.min(tasks);
        addToMinVM(val, vms);
        tasks.remove(val);
    }

    public static void addToMinVM(Integer value, List<List<Integer>> vms){
        Integer min_sum = Integer.MAX_VALUE;
        int min_index = 0;
        for(int i = 0; i < vms.size(); i++){
            List<Integer> vm = vms.get(i);
            Integer sum = 0;
            for(Integer val : vm) {
                sum += val;
            }
            if(sum < min_sum){
                min_sum = sum;
                min_index = i;
            }
        }
        vms.get(min_index).add(value);
    }

    public static double calculateLoadBalance(List<List<Integer>> vms) {
        // variance in size of each machine, lower is better
        int n = vms.size();
        double meanSize = 0;
        for (List<Integer> machine : vms) {
            meanSize += machine.size();
        }
        meanSize /= n;

        double variance = 0;
        for (List<Integer> machine : vms) {
            int machineSize = machine.size();
            variance += (machineSize - meanSize) * (machineSize - meanSize);
        }
        variance /= (n - 1);

        return variance;
    }
}


