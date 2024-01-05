import os
import sys
from pathlib import Path
from collections import defaultdict

experiment_id = sys.argv[1]
request_counter = int(sys.argv[2])
API = sys.argv[3]

cwd = os.getcwd()
project_path = cwd.split('SURF')[0] + 'SURF/'

def guess_path_to_alternative_subgraph_file(API:str) -> Path:
    base_dir = project_path + "code/graphs/"
    file_name = API + "_formatted_onlylabelled_" + experiment_id + str(request_counter) + "_result_alternative_subgraphs.txt"
    return base_dir + file_name

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
    file_name = API + "_formatted_onlylabelled_" + experiment_id + str(request_counter) + "_result"

    return base_dir + file_name

def read_features_file(API):
    features = defaultdict(int)
    feature_ids = []
    with open(project_path + "code/graphs/" + API + "_formatted_onlylabelled_" + experiment_id + str(request_counter) + "_result_features.txt", 'r') as f:
        for line in f:
            line = line.strip()
            is_first_line   = line.startswith("graph_id")
            
            if not is_first_line:
                splitted = line.split(',')

                is_correct = splitted[1] == '1'
                for feature_i, feature in enumerate(splitted[2:]):
                    # print(feature_i, feature_ids)
                    feature_id = feature_ids[int(feature_i)]
                    if feature == '1':
                        if is_correct: 
                            features[feature_id] += 1 # associated with correct use
                        else:
                            features[feature_id] -= 1 # associated with misuse

            else:
                splitted = line.split(',')
                for feature in splitted[2:]:
                    feature_ids.append(feature)
                # print(feature_ids)
    # print(features)

    features = {int(feature.split('_')[1]): value for feature, value in features.items()}
    return features

def debug2():
    global request_counter
    best_subgraphs = {}

    # API_under_test = "java.security.MessageDigest__digest"
    API_under_test = API
    if API_under_test.endswith("true"):
        raise Exception("accidentally ended API's name with true")

    # pick the highest numbered formmated file
    while not os.path.exists( guess_path_to_alternative_subgraph_file (API_under_test)):
        request_counter -= 1
        if request_counter == 0:
            return

    path = guess_path_to_alternative_subgraph_file(API_under_test)
    # print(path)
    # print(feature_to_correct_count)

    highest_score = 0
    with open(path, 'r') as f:
        for line in f:
            line = line.strip()
            if line:
                splitted = line.split(',')
                score = float(splitted[1])

                best_subgraphs[int(splitted[0])] = score
                if score > highest_score:
                    highest_score = score


    
    sorted_best_subgraphs = sorted(best_subgraphs.items(), key=lambda x: x[1], reverse=True)
    # filter away the ones that are not the highest score
    # sorted_best_subgraphs = [x for x in best_subgraphs.items() if x[1] == highest_score]
    
    sorted_best_subgraphs = [x[0] for x in sorted_best_subgraphs][:200]
    # print(sorted_best_subgraphs)

    convert(path_to_vert_map_file(API_under_test),
            path_to_edge_map_file(API_under_test),
            path_to_frequent_subgraph_file(API_under_test),
            sorted_best_subgraphs,
            )


def convert(vertmap_path:str, edge_map_path:str, graph_path:str, only_show:list):
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

    with open(graph_path, 'r') as graph_file:
        current_id = -1
        always_show = not only_show

        current_graph_vertices = {}

        for line in graph_file:
            line = line.strip()
            if line.startswith('t'):
                splitted = line.split(' ')
                current_id = int(splitted[2])

                current_graph_vertices = {}

                if always_show or current_id in only_show:
                    content.setdefault(current_id, '')
                    content[current_id] += line + '\n'

            elif line.startswith('v'):
                splitted = line.split(' ')
                if always_show or current_id in only_show:
                    content[current_id] += "v " + splitted[1] + " " + vertex_map[int(splitted[2])] + "\n"
                current_graph_vertices[int(splitted[1])] = int(splitted[2])

            elif line.startswith('e'):
                splitted = line.split(' ')
                if always_show or current_id in only_show:
            
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
    
    # only_show = [x[0] for x in sorted(frequency, key=lambda x: x[1], reverse=True)[:100]]
    for item in only_show:
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

        for from_node in edges_from:
            render_from_node = nodes[from_node] #.replace('<', "[").replace('>', "]")
            # if not edges_from[from_node]:
            #     print(f"{render_from_node}", end='')
            #     print()
            for to_node in edges_from[from_node]:
                render_to_node = nodes[to_node[0]] # nodes[to_node[0]].replace('<', "[").replace('>', "]")
                if 'order_rev' in to_node[1]:
                    print(f"{render_to_node} -> {render_from_node} (order)", end=',')
                else:
                    print(f"{render_from_node} -> {render_to_node} ({to_node[1]})", end=',')
                print()
        # print()
            
debug2()

