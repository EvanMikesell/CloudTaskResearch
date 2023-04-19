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
public class EHAMMSimulation extends HAMMSimulation {

    /**
     * The cloudlet list.
     */
    private static List<Cloudlet> cloudletList;

    /**
     * The vmlist.
     */
    private static List<Vm> vmlist;

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {

        Log.printLine("Starting EHAMM Simulation...");

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
            for (int i = 1; i <= vmCount; i++) {
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
            for (int i = 0; i < numTasks; i++) {
                tasks.add(rand.nextInt(100000));
            }

            //Next, we will do the HAMM algorithm to decide which tasks should be bound to which VM
            List<List<Integer>> vms = HAMM(tasks, vmCount);
            System.out.println(vms);

            //Next, we reschedule the tasks
            vms = reschedule(vms);

            //Next, we will bind our cloudlets to the VMs, and run the simulation
            int id = 0;

            // create a list of lists for cloudlets, each list is a VM
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

            Log.printLine("EHAMM Simulation finished!");

        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    public static List<List<Integer>> reschedule(List<List<Integer>> vms) {
        List<List<Integer>> highLoad = new ArrayList<>();
        List<List<Integer>> lowLoad = new ArrayList<>();

        highAndLowSplit(vms, highLoad, lowLoad);

        /*
        Rescheduling takes the smallest tasks from the largest machines and attempts to move them
        to the smallest load machines.
        */
        while (!highLoad.isEmpty()) {
            List<Integer> highLoadMachine = Collections.max(highLoad, Comparator.comparing(List::size));
            int smallestTask = Collections.min(highLoadMachine);
            List<Integer> smallestLoadMachine = Collections.min(lowLoad, Comparator.comparing(List::size));

            int smallestLoadMachineSize = smallestLoadMachine.stream().mapToInt(Integer::intValue).sum();
            int highLoadMachineSize = highLoadMachine.stream().mapToInt(Integer::intValue).sum();
            int difference = highLoadMachineSize - smallestLoadMachineSize;

            int differenceAfterRescheduling = (highLoadMachineSize - smallestTask) - (smallestLoadMachineSize + smallestTask);

            // if moving the task results in a smaller difference in overall loads, then move them
            // if not, remove the current machine from our list of high load machines
            if (Math.abs(difference) > Math.abs(differenceAfterRescheduling)) {
                smallestLoadMachine.add(smallestTask);
                highLoadMachine.remove(Integer.valueOf(smallestTask));
            } else {
                lowLoad.add(highLoadMachine);
                highLoad.remove(highLoadMachine);
            }
        }
        return lowLoad;
    }

    private static void highAndLowSplit(List<List<Integer>> vms, List<List<Integer>> highLoad, List<List<Integer>> lowLoad) {
        List<Double> averages = new ArrayList<>();
        for (List<Integer> vm : vms) {
            double average = vm.stream().mapToInt(Integer::intValue).average().orElse(0.0);
            averages.add(average);
        }
        double overallAvg = averages.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        for (int i = 0; i < vms.size(); i++) {
            List<Integer> vm = vms.get(i);
            double average = averages.get(i);
            if (overallAvg > average) {
                highLoad.add(vm);
            } else {
                lowLoad.add(vm);
            }
        }
    }
}