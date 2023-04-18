import random
import math

"""
Compares the performance of the HAMM algorithm and the EHAMM algorithm.
We are concerned with the makespan as well as the load variance between machines.
"""


def ehamm(num_tasks=1000, num_vms=6, task_size_cap=1000):
    tasks = [random.randint(1, task_size_cap) for x in range(0, num_tasks)]
    vms = [[] for x in range(0, num_vms)]  # can set however many VMs you want

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

    # find original makespan and variance in loads
    makespan, variance = get_results(vms)
    print("HAMM Scheduling:")
    print("Makespan:", makespan)
    print("Variance in load sizes:", variance)


    # Compare to the rescheduled results
    rescheduled_vms = reschedule(vms)
    rescheduled_makespan, rescheduled_variance = get_results(rescheduled_vms)
    print("EHAMM Scheduling")
    print("Makespan:", rescheduled_makespan)
    print("Variance in load sizes:", rescheduled_variance)
    print()

def reschedule(vms):
    high_load, low_load = high_and_low_split(vms)

    while high_load:
        high_load.sort()
        high_load_machine = max(high_load)
        smallest_task = min(high_load_machine)
        smallest_load_machine = min(low_load)

        # find average
        smallest_load_machine_size = sum(smallest_load_machine)
        high_load_machine_size = sum(high_load_machine)
        difference = high_load_machine_size - smallest_load_machine_size

        difference_after_rescheduling = (high_load_machine_size - smallest_task) - (smallest_load_machine_size + smallest_task)
        if abs(difference) > abs(difference_after_rescheduling):
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

def calculate_makespan(vms):
    makespan = 0
    for machine in vms:
        makespan = max(makespan, sum(machine))
    return makespan


def calculate_load_balance(vms):
    n = len(vms)
    mean_size = sum(len(lst) for lst in vms) / n
    variance = sum((len(lst) - mean_size) ** 2 for lst in vms) / (n - 1)
    return variance


def get_results(vms):
    makespan = calculate_makespan(vms)
    load_balance = calculate_load_balance(vms)
    return makespan, load_balance


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


if __name__ == '__main__':
    ehamm()
