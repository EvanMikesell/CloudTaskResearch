import random
import math

"""
Testing to see if the HAMM Algorithm (Hybrid Algorithm of Min-Min and Max-Min) can be improved.
We will look to see if the virtual machines have balanced loads at the end of the algorithm.
Can look into ways to balance these virtual machines.
"""


def hamm():
    num_tasks = 1000
    num_vms = 6
    tasks = [random.randint(1, 1000) for x in range(0, num_tasks)]
    vms = [[] for x in range(0, num_vms)]  # can set however many VMs you want
    #tasks = [20, 30, 40, 50, 60, 70, 80, 90, 100, 120, 1, 20, 20, 30, 40, 50, 60, 70, 80, 90]
    #vms = [[] for x in range(0, 3)]

    while tasks:
        avg = sum(tasks) / len(tasks)
        lower = 0
        higher = 0

        # count averages
        for task in tasks:
            if task <= avg:
                lower += 1
            else:
                higher += 1

        if lower >= higher:
            max_min(tasks, vms)
        else:
            min_min(tasks, vms)

    print(vms)
    rescheduled_vms = reschedule(vms)
    print_results(vms, rescheduled_vms)
    print(vms)
    print(rescheduled_vms)


def reschedule(vms):
    high_load, low_load = high_and_low_split(vms)

    print("HIGH VS LOW LOAD")
    print(high_load)
    print(low_load)

    while high_load:
        high_load.sort()
        for i in range(len(high_load)-1, -1, -1):
            high_load_machine = high_load[i]
            smallest_task = min(high_load_machine)
            smallest_load_machine = min(low_load)
            print(high_load_machine, smallest_task)

            # find average
            smallest_load_machine_size = sum(smallest_load_machine)
            high_load_machine_size = sum(high_load_machine)
            difference = high_load_machine_size - smallest_load_machine_size

            difference_after_rescheduling = (high_load_machine_size - smallest_task) - (smallest_load_machine_size + smallest_task)
            print(difference, difference_after_rescheduling)
            if abs(difference) > abs(difference_after_rescheduling):
                print("rescheduling")
                smallest_load_machine.append(smallest_task)
                high_load_machine.remove(smallest_task)
            else:
                low_load.append(high_load_machine)
                high_load.remove(high_load_machine)
    return low_load


def high_and_low_split(vms, threshold=0.02):
    averages = []
    high_load = []
    low_load = []
    for vm in vms:
        averages.append(sum(vm) / len(vm))
    overall_avg = sum(averages) / len(averages)
    for i, average in enumerate(averages):
        #variance = (overall_avg / average)
        if overall_avg > average:
            high_load.append(vms[i])
        else:
            low_load.append(vms[i])
    return high_load, low_load


def print_results(original_vms, rescheduled_vms):
    original_makespan = calculate_makespan(original_vms)
    rescheduled_makespan = calculate_makespan(rescheduled_vms)
    original_variance = calculate_load_balance(original_vms)
    rescheduled_variance = calculate_load_balance(rescheduled_vms)

    print("Original Makespan:", original_makespan)
    print("Rescheduled Makespan:", rescheduled_makespan)

    print("Original Variance:", original_variance)
    print("Rescheduled Variance:", rescheduled_variance)



def calculate_makespan(vms):
    makespan = 0
    for machine in vms:
        makespan = max(makespan, sum(machine))
    return makespan


def calculate_load_balance(vms):
    # calculate average load
    loads = []
    for vm in vms:
        avg = sum(vm) / len(vm)
        loads.append(avg)
    return -1


def max_min(tasks, vms):
    val = max(tasks)
    add_to_min_vm(val, vms)
    tasks.remove(val)


def min_min(tasks, vms):
    val = min(tasks)
    add_to_min_vm(val, vms)
    tasks.remove(val)


def add_to_min_vm(val, vms):
    min_vm = math.inf
    vm_to_assign = 0
    for i, vm in enumerate(vms):
        if sum(vm) < min_vm:
            vm_to_assign = i
            min_vm = sum(vm)
    vms[vm_to_assign].append(val)


def print_vm_info(num_tasks, num_vms, vms):
    # printing information on the VMs state
    for vm in vms:
        print(vm)
    overall = 0
    max_vm = 0
    min_vm = math.inf
    for i, vm in enumerate(vms):
        vm_sum = sum(vm)
        max_vm = max(max_vm, vm_sum)
        min_vm = min(min_vm, vm_sum)
        print("VM ", i, vm_sum)
        overall += vm_sum
    average_vm_load = overall / num_vms
    average_task_size = overall / num_tasks
    print("Min VM: ", min_vm)
    print("Max VM: ", max_vm)
    print("Difference from Min to Average: ", 1 - (min_vm / average_vm_load))
    print("Difference from Min to Max: ", 1 - (min_vm / max_vm))
    print("Average VM Load: ", average_vm_load)
    print("Average Task Size: ", average_task_size)


hamm()
