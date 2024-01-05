import os
from pathlib import Path
from collections import defaultdict
import sys

experiment_id = sys.argv[1]
request_counter = int(sys.argv[2])
API = sys.argv[3]
focus_node = sys.argv[4]



def path_to_vert_map_file(API:str) -> str:
    base_dir = project_path + "code/graphs/"
    file_name = API + "_vertmap.txt"

    return base_dir + file_name

def path_to_edge_map_file(API:str) -> str:
    base_dir = project_path + "code/graphs/"
    file_name = API + "_edgemap.txt"

    return base_dir + file_name

def path_to_frequent_subgraph_file(API:str) -> str:
    base_dir = project_path + "code/graphs/"
    # file_name = API + "_formatted_onlyunlabelled_" + experiment_id + str(request_counter) +".txt_result"
    file_name = API + "_formatted_onlyunlabelled_" + experiment_id + str(request_counter) +"_result"

    return base_dir + file_name

def debug2():
    global request_counter
    best_subgraphs = {}

    # API_under_test = "java.security.MessageDigest__digest"
    API_under_test = API
    if API_under_test.endswith("true"):
        raise Exception("accidentally ended API's name with true")

    with open(project_path + 'code/graphs/' + API + '_vertmap.txt', 'r') as infile:
        lines = infile.readlines()
        focus_verts = []
        for line in lines:
            if focus_node in line:
                node_id = line.split(',')[1].strip()
                try:
                    focus_verts.append(int(node_id))
                except ValueError:
                    continue
                # print(line)
                # focus_vert = line.split(',')[2].strip()
                # break
        # pick the highest numbered formmated file
    while not os.path.exists( path_to_frequent_subgraph_file (API_under_test)):
        # print('cant find output for request_counter= ' + str(request_counter) + ' for API ' + API_under_test + '... ' + path_to_frequent_subgraph_file (API_under_test))
        request_counter -= 1
        
        if request_counter == 0:
            break
        

    convert(path_to_vert_map_file(API_under_test),
            path_to_edge_map_file(API_under_test),
            path_to_frequent_subgraph_file(API_under_test),
            focus_verts
            )


def convert(vertmap_path:str, edge_map_path:str, graph_path:str, focus_verts:str):
    vertex_map = {}
    line_i = 0
    
    with open(vertmap_path, 'r') as vert_map:
        for line in vert_map:
            split_at = line.rindex(',')
            try:
                vertex_map[int(line[split_at + 1:])] = line[:split_at]
            except ValueError:
                print(f"weird input format. string={line} at line {line}")
                raise
            line_i += 1
    vertex_map[-1] = 'MISSING'

    edge_map = {}
    with open(edge_map_path, 'r') as edge_map_file:
        for line in edge_map_file:
            split_at = line.rindex(',')
            edge_map[int(line[split_at + 1:])] = line[:split_at]


    content = {}

    frequency = []

    # if len(focus_verts) == 0:
    #     # sys.stderr.write("focus node not in graph" + focus_vert)
    #     # we will print only "order"
    #     show_only_order = True
    # else:
    #     show_only_order = False

    # focus_verts = [vertex_map[x] for x in focus_verts]

    with open(graph_path, 'r') as graph_file:
        current_id = -1

        current_graph_vertices = {}
        for line in graph_file:
            line = line.strip()
            if line.startswith('t'):
                # print('has graphs')
                splitted = line.split(' ')
                current_id = int(splitted[2])
        
                times = int(splitted[4])
                frequency.append((current_id, times))

                current_graph_vertices = {}

                content[current_id] =  ''
                content[current_id] += line + '\n'

            elif line.startswith('v'):
                splitted = line.split(' ')
                content[current_id] += "v " + splitted[1] + " " + vertex_map[int(splitted[2])] + "\n"
                current_graph_vertices[int(splitted[1])] = int(splitted[2])

            elif line.startswith('e'):
                splitted = line.split(' ')
            
                edge_name = edge_map[int(splitted[3])] 
                from_edge_index = int(splitted[1])
                to_edge_index = int(splitted[2])
                from_edge =  current_graph_vertices[from_edge_index]
                to_edge   =  current_graph_vertices[to_edge_index]

                if '_lower_to_higher' in edge_name:
                    edge_name = edge_name.replace('_lower_to_higher', '')
                    # check if from_edge and to_edge are from lower to higher
                    if from_edge > to_edge:
                        from_edge_index, to_edge_index = to_edge_index, from_edge_index

                elif '_higher_to_lower' in edge_name:
                    edge_name = edge_name.replace('_higher_to_lower', '')
                    # check if from_edge and to_edge are from higher to lower
                    if from_edge < to_edge:
                        from_edge_index, to_edge_index = to_edge_index, from_edge_index                        

                content[current_id] += "e " + str(from_edge_index) + " " + str(to_edge_index) + " " + edge_name + "\n"

            else:
                continue
    
    sorted_rows = [x[0] for x in sorted(frequency, key=lambda x: x[1], reverse=True)]
    # print('focus_vert is ', focus_vert)

    # if len(focus_verts) == 0:
    #     # sys.stderr.write("focus node not in graph" + str(focus_verts))
    #     # we will print only "order"
    #     # show_only_order = True
    #     pass
    # else:
    #     # sys.stderr.write("focus node in graph" + str(focus_verts))
    #     # show_only_order = False
    #     pass
    

    for item in sorted_rows:
        # print(content[item])
        nodes = {}
        edges_from = defaultdict(list)

        for row in content[item].split('\n'):
            if row.startswith('v'):
                # print(row.split(' ')[2]))
                nodes[int(row.split(' ')[1])] = row.split(' ')[2]
                edges_from[int(row.split(' ')[1])] = []
            elif row.startswith('e'):
                # print(row.split(' ')[3])
                edges_from[int(row.split(' ')[1])].append((int(row.split(' ')[2]), row.split(' ')[3]))

        intersection = set(focus_verts).intersection(set(nodes.values()))
        # if not show_only_order and 
        # if len(intersection) == 0:
            # sys.stderr.write("focus node not in graph" + focus_vert)
            # print("focus node not in graph" + str(focus_verts) + ', graphs contains ' + str(nodes.values()))
            # continue

        for from_node in edges_from:
            render_from_node = nodes[from_node] #.replace('<', "[").replace('>', "]")
            # if not edges_from[from_node]:
            #     print(f"{render_from_node}", end='')
            #     print()
            for to_node in edges_from[from_node]:
                render_to_node = nodes[to_node[0]] # nodes[to_node[0]].replace('<', "[").replace('>', "]")
                # if show_only_order:
                #     if 'order' not in to_node[1]:
                #         continue
                # else:
                #     if 'order' in to_node[1]:
                #         continue
                        
                if 'order_rev' in to_node[1]:
                    print(f"{render_to_node} -> {render_from_node} (order)", end=',')
                else:
                    print(f"{render_from_node} -> {render_to_node} ({to_node[1]})", end=',')
                print()
        # print()






debug2()

