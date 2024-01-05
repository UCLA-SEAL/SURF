import sys
import subprocess
import json
from base64 import b64decode
import os
import time
import datetime


print(sys.argv)
target_graph_ids = sys.argv[1]
target_label = sys.argv[2]

encoded_element_id_to_graph_id = sys.argv[3]

experiment_id = sys.argv[4]
request_counter = int(sys.argv[5])

API = sys.argv[6]

focalNode = sys.argv[7]

nodesToInclude = sys.argv[8]
nodesToInclude = nodesToInclude.split(',')

now = datetime.datetime.now()

element_id_to_graph_id =  json.loads(b64decode(encoded_element_id_to_graph_id).decode('utf-8'))
graphs_to_mine = [str(graph_id) for graph_id in element_id_to_graph_id.values()]

label_counts = {'+': 0, '-': 0, 'U' : 0}

cwd = os.getcwd()
project_path = cwd.split('SURF')[0] + 'SURF/'

if request_counter >= 1:
    time_counter = 0
    while not os.path.exists( project_path + 'code/graphs/' + API + '_formatted_' + experiment_id + str(request_counter - 1) + '.txt' ):
        time.sleep(1)
        time_counter += 1
        if time_counter > 2:
            break
    # pick the highest numbered formmated file
    while not os.path.exists( project_path + 'code/graphs/' + API + '_formatted_' + experiment_id + str(request_counter - 1) + '.txt' ):
        request_counter -= 1
        if request_counter == 0:
            break
        
    with open(project_path + 'code/graphs/' + API + '_formatted_' + experiment_id + str(request_counter - 1) + '.txt', 'r') as infile:
        lines = infile.readlines()
else:
    if os.path.exists(project_path + 'code/graphs/' + API + '_formatted_' + experiment_id + '.txt'):
        with open(project_path + 'code/graphs/' + API + '_formatted_' + experiment_id + '.txt', 'r') as infile:
            lines = infile.readlines()
    else:
        with open(project_path + 'code/graphs/' + API + '_formatted.txt', 'r') as infile:
            lines = infile.readlines()


# add newly added examples
if os.path.exists(project_path + 'code/graphs/' + API + '_' + 'test_formatted_' + '.txt'):
    # determine the number of graphs in the "static" file
    num_graphs = 0
    for line in lines:
        if line.startswith('t'):
            num_graphs += 1


    new_example_lines = []
    with open(project_path + 'code/graphs/' + API + '_' + 'test_formatted_' + '.txt', 'r') as infile:
        new_example_lines = infile.readlines()

    # postprocess the graph ids
    for i in range(len(new_example_lines)):
        if new_example_lines[i].startswith('t'):
            new_example_lines[i] = new_example_lines[i].replace('0', str(num_graphs))

    lines += new_example_lines


target_graph_ids = target_graph_ids.split(',')

with open(project_path + 'code/graphs/' + API + '_formatted_' + experiment_id + str(request_counter) + '.txt.tmp', 'w') as outfile,\
        open(project_path + 'code/graphs/' + API + '_formatted_onlylabelled_' + experiment_id + str(request_counter)+ '.txt', 'w') as outfile1,\
        open(project_path + 'code/graphs/' + API + '_formatted_onlyunlabelled_' + experiment_id + str(request_counter)+ '.txt', 'w') as outfile2:
    current_label = None
    for line in lines:
        if not line.startswith('t'):
            outfile.write(line)
            if current_label == 'U' and graph_id in graphs_to_mine and '-' not in line:
                outfile2.write(line)
            
            if current_label != 'U':
                outfile1.write(line)
                if  '-' not in line:
                    outfile2.write(line)

            if (current_label == 'U' or graph_id not in graphs_to_mine) and '-' in line:
                # add a dummy node
                outfile1.write('v 0 -1\n')
                outfile1.write(line)


            if '-'  in line:   # end of one labelled graph
                if graph_id in graphs_to_mine:
                    outfile2.write(line)
                else:
                    outfile2.write('v 0 -1\n')
                    outfile2.write(line)
            # if (current_label != 'U' or graph_id not in graphs_to_mine) and '-' in line:
            #     # add a dummy node for the unlabeled graphs file
            #     outfile2.write('v 0 -1\n')
            #     outfile2.write(line)

            continue
            
        components = line.split()
        graph_id = components[2]

        # print(components)
        if graph_id in target_graph_ids:
            if target_label == 'positive':
                target_label = '+'
            elif target_label == 'negative':
                target_label = '-'

            components[3] = target_label

        #update label_counts
        label_counts[components[3]] += 1

        line = ' '.join(components) + '\n'        
        outfile.write(line)
        
        current_label = components[3]
        # if current_label != 'U':
        if graph_id in graphs_to_mine:
            outfile1.write(line)
        else:
            outfile1.write(line.replace('+', 'U').replace('-', 'U'))
            current_label = 'U'
    
        if current_label == 'U' and graph_id in graphs_to_mine:
            outfile2.write(line.replace('U', 'U'))
        else:
            outfile2.write(line.replace('+', '+').replace('-', 'U'))


must_include_node_ids = []
exclude_node_ids = []
with open(project_path + 'code/graphs/' + API + '_vertmap.txt', 'r') as infile:
    lines = infile.readlines()
    focal_node_id = None
    for line in lines:
        if focalNode in line:
            focal_node_id = line.split(',')[1].strip()
            # break

        if line.startswith('<return>') or line.startswith('<catch>') or line.startswith('UNKNOWN,'):
            exclude_node_ids.append(line.split(',')[1].strip())

        if nodesToInclude and line.split(',')[0].strip().replace('.', '__') in nodesToInclude:
            must_include_node_ids.append(line.split(',')[1].strip())

exclude_node_ids = ','.join(exclude_node_ids)

print('must_include_node_ids')
print(must_include_node_ids)

print('focal_node_id')
print(focal_node_id)

has_enough_labels = label_counts['+'] >= 1 or label_counts['-'] >= 1
if has_enough_labels:

    # shell out to java to run the code
    # java -Xmx64G -jar GSpanMiner.jar -d java.lang.String__charAt__1_formatted.txt -b java.lang.String__charAt__1_vertmap.txt -s 3 -a 6
    # run java
    print('running java (miner for labelled graphs)')
    if focal_node_id:
        must_include_node_ids.append(focal_node_id)
    must_include_node_ids = ','.join(must_include_node_ids)
    
    # target_subgraph_size = must_include_node_ids.count(',') + 2
    target_subgraph_size = 2

    print((' '.join(["java",'-Xmx32G', "-jar" ,project_path + 'code/meteor_app/misc_scripts/subgraph_miner.jar', '-d' , project_path + 'code/graphs/' + API + '_formatted_onlylabelled_' + experiment_id + str(request_counter) + '.txt',  '-s', '1', '-a', str(target_subgraph_size), '-i', '1', '-m', must_include_node_ids, '-e', exclude_node_ids])), file=sys.stderr)
    p1 = subprocess.Popen(["java",'-Xmx32G', "-jar" ,project_path + 'code/meteor_app/misc_scripts/subgraph_miner.jar', '-d' , project_path + 'code/graphs/' + API + '_formatted_onlylabelled_' + experiment_id + str(request_counter) + '.txt',  '-s', '1', '-a', str(target_subgraph_size), '-i', '1', '-m', must_include_node_ids, '-e', exclude_node_ids])
    # for line in p.stdout.readlines():
    #     print(line)
    

else:
    print('not enough labelled examples', label_counts)


support = 1
    
# print('running java (miner for unlabelled   graphs)', ' '.join(["java",'-Xmx32G', "-jar" ,project_path + 'code/meteor_app/misc_scripts/subgraph_miner.jar', '-d' , project_path + 'code/graphs/' + API + '_formatted_onlyunlabelled_' + experiment_id + str(request_counter) + '.txt' , '-s', str(support), '-a', '2', '-i', '1']), file=sys.stderr)

# p2 = subprocess.Popen(["java",'-Xmx32G', "-jar" ,project_path + 'code/meteor_app/misc_scripts/frequent_subgraph_miner.jar', '-d' , project_path + 'code/graphs/' + API + '_formatted_onlyunlabelled_' + experiment_id + str(request_counter) + '.txt' , '-s', str(support), '-a', '2', '-i', '1'])
p2 = subprocess.Popen(["java",'-Xmx32G', "-jar" ,project_path + 'code/meteor_app/misc_scripts/subgraph_miner.jar', '-d' , project_path + 'code/graphs/' + API + '_formatted_onlyunlabelled_' + experiment_id + str(request_counter) + '.txt' , '-s', str(support), '-a', '2', '-i', '1'])
# for line in p.stdout.readlines():
#     print(line)
p2.wait()
if has_enough_labels:
    p1.wait()

os.rename(project_path + 'code/graphs/' + API + '_formatted_' + experiment_id + str(request_counter) + '.txt.tmp', project_path + 'code/graphs/' + API + '_formatted_' + experiment_id + str(request_counter) + '.txt')

print('end' , now)