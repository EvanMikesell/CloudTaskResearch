import random
import math

"""
Testing to see if the HAMM Algorithm (Hybrid Algorithm of Min-Min and Max-Min) can be improved.
We will look to see if the virtual machines have balanced loads at the end of the algorithm.
Can look into ways to balance these virtual machines.
"""

def HAMM():
    num_tasks = 100
    num_vms = 6
    tasks = [random.randint(1, 50) for x in range(0, num_tasks)]
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

    # printing information on the VMs state
    for vm in vms:
        print(vm)
    overall = 0
    for i, vm in enumerate(vms):
        vm_sum = sum(vm)
        print("VM ", i, vm_sum)
        overall += vm_sum

    average_vm_load = overall / num_vms
    average_task_size = overall / num_tasks
    print("Average VM Load: ", average_vm_load)
    print("Average Task Size: ", average_task_size)


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


HAMM()
