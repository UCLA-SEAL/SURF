# go through the list of python files in the directory ~/repos/active_learning_interface/CryptoAPI-Bench
import glob
import json
import re 
import os
import shutil


cwd = os.getcwd()
project_path = cwd.split('SURF')[0] 



def get_all_java_files():
    return glob.glob(project_path + '/GithubExamples/SecureRandom/*.java', recursive=True)

def extract_url(file_path):
    # print(file_path + ' ' + file_path.split('CryptoAPI-Bench/') [1])
    return 'dummy'

def extract_raw_code(content):
    # find method-level code containing the API call to Random
    pattern = r'((public|private|protected).*\s*Random\s*.*\(.*\).*})'
    match = re.search(pattern, content, re.DOTALL)
    if match:
        
        return match.group(0)
    else:
        if 'Random' in content:
            print('failed to match the following code :', content)
        return ''

test_files = ['17_PulsarHandler.java', 'Test_SO2.java', 'Test_SO3.java', 'Test_RSASig.java', 'Test_RSATool.java', 'Test_NeteaseRSATool.java']


def convert_to_json(files):
    json_data = []  # list to hold JSON objects for each file
    count = 1000
    print(len(files))
    for java_file in files:
        
        with open(java_file, 'r') as f:
            # read the content of the file
            content = f.read()
            # extract the required information from the content
            url = extract_url(java_file)
            raw_code = extract_raw_code(content)
            example_id = count
            dataset = 'random'

            # reject the example if it does not contain the required API call
            if 'Random' not in raw_code:
                print('skipping', java_file)
                continue


 
            # create a JSON object with the extracted data
        
            json_obj = {
                'url': url,
                'rawCode': raw_code,
                'exampleID': example_id,
                'dataset': dataset,
                'filepath': java_file
            }

            if java_file.split('/')[-1] in test_files:
                json_obj['test'] = True
                print('test!')
                
            json_data.append(json_obj)
            count+=1

            print('done', java_file)
    print(count)
    # return the list of JSON objects as a JSON array
    return json.dumps(json_data)



    
json_obj = convert_to_json(get_all_java_files())

with open('cryptoapi_bench_random2.json', 'w') as f:
    f.write(json_obj)
print('wrote to the present directory. Move it to where Active learning interface expects it to be.')


# next, move the source files into /Users/.../repos/active_learning_interface/SURF/code/meteor_app/full_source/cryptoapi_bench_SecureRandom
if not os.path.exists(project_path + '/SURF/code/meteor_app/full_source/cryptoapi_bench_SecureRandom'):
    os.mkdir(project_path + '/SURF/code/meteor_app/full_source/cryptoapi_bench_SecureRandom')
for java_file in json.loads(json_obj):
    # print(java_file)
    example_id = java_file['exampleID']
    filepath = java_file['filepath']
    if not os.path.exists(project_path + '/SURF/code/meteor_app/full_source/cryptoapi_bench_SecureRandom/' + str(example_id)):
        os.mkdir(project_path + '/SURF/code/meteor_app/full_source/cryptoapi_bench_SecureRandom/' + str(example_id))
    shutil.copyfile(filepath, project_path + '/SURF/code/meteor_app/full_source/cryptoapi_bench_SecureRandom/' + str(example_id) + '/' + os.path.basename(filepath))

print('wrote to ' + project_path + '/SURF/code/meteor_app/full_source/cryptoapi_bench_SecureRandom')
print('done')