import { Meteor } from 'meteor/meteor';
import { EJSON } from 'meteor/ejson';
import { Session } from 'meteor/session';
// import { TestExamples } from '../imports/api/options';

const { Configuration, OpenAIApi } = require("openai");


export const Examples = new Mongo.Collection('examples');
export const TestExamples = new Mongo.Collection('testexamples');
export const ActionLog = new Mongo.Collection('actionlog');
export const Subgraphs = new Mongo.Collection('subgraphs');
export const Bags = new Mongo.Collection('bags');
export const Queries = new Mongo.Collection('queries');
export const Config  = new Mongo.Collection('config');
export const History  = new Mongo.Collection('history');

var request_counter = process.env.STARTING_COUNT ? parseInt(process.env.STARTING_COUNT) : 1;
var experiment_id = 'test';

// var APIshortName = 'digest';
// var API = 'java.security.MessageDigest__digest';
var APIshortName = "init";
var API = "javax.crypto.Cipher__init";

APIshortName = process.env.API_SHORTNAME ? process.env.API_SHORTNAME : APIshortName;
API = process.env.TARGET_API ? process.env.TARGET_API : API;; 

var showStreamlined = process.env.SHOW_STREAMLINED ? process.env.SHOW_STREAMLINED : true;

console.log('inserting config');


// var appPath = appPath + ;

// get current path
var path = Npm.require('path');
var projectPath = path.resolve('.').split('SURF')[0] + 'SURF/';
var appPath = projectPath + "/code/meteor_app/";

// var APIshortName = "random";
// var API = "java.security.SecureRandom__Key";

// var APIshortName = 'digest';
// var API = 'java.security.MessageDigest__digest';

const configuration = new Configuration({
  apiKey: process.env.OPENAI_API_KEY,
});
const openai = new OpenAIApi(configuration);


Meteor.startup(() => {
  var exec = Npm.require("child_process").exec;
  var Future = Npm.require("fibers/future");

  //to load new data into the database, run this command:
  //mongoimport --db test --collection <collectionName> --drop --file ~/downloads/<data_dump>.json
  console.log('start-up code running');
  console.log(Examples.find().count());

  var reload = true; 
  // var reload = false;
  if (reload){
    resetDatabase();
    Config.insert({APIshortName: APIshortName, API: API, showStreamlined: showStreamlined});

  }

});

var shellOutToReadSubgraphs = function(request_number, focalNode, eraseOld, showImmediately) {

  // reset the subgraphs, but do not clear the bagged patterns
  // or the patterns that are already labeled
  if (focalNode == 'pseudo-node' || eraseOld) {
  
    // console.log('clear  subgraphs and more');
    var  a = Subgraphs.remove({'$and': [{ 'labelled': {'$ne': true} }, {'hint':{'$ne': true} } ,{ '$or': [ {'bag' : {'$exists': false}}, {'bag': {$eq: null}}  ]}]});
    console.log(a);
  } 


  spawn = Npm.require('child_process').spawn;

  // console.log('reading subgraphs');
  // console.log('request_number', request_number);
  
  command = spawn('python3',[appPath + "misc_scripts/debug_subgraphs.py", experiment_id, request_number, API]);
  // console.log('python3',[appPath + "misc_scripts/debug_subgraphs.py", experiment_id, request_number, API].join(' '));

  // remove 'alternative' subgraphs
  Subgraphs.remove({'$and': [{'alternative': true}, { 'labelled': {'$ne': true} }, { '$or': [ {'bag' : {'$exists': false}}, {'bag': {$eq: null}}  ]}]});

  // how many user feedback has been given?
  var numUserFeedback = History.find({}).count();

  command.stdout.on('data',  Meteor.bindEnvironment(function (data) {
    data = data.toString();

    Subgraphs.find({discriminative:true, hidden:true, labelled:false}).forEach(function(subgraph) {
      // console.log('[discriminative=true] removing subgraph: ' + subgraph.rawText);
      Subgraphs.remove({_id: subgraph._id});
    });

    // console.log('subgraphs read: ' + data.toString());
    // var text = '';
    for (var i=0; i<data.split('\n').length; i++){
      var text = data.split('\n')[i];
      var edges = [];
      var adjlist = {};

      var isPattern = null;
      if (data.split('\n')[i].length != 0) {
        if (text.length != 0) {

          if (text.split(',').length >= 2 ) {
            var fragments = text.split(',');
            

            for (var j=0; j<fragments.length; j++) {

              if (fragments[j].trim().length == 0) {
                continue;
              }
              // the last fragment is just + or -
              if (fragments[j].trim().length == 1) {
                if (fragments[j].trim() == '+' || fragments[j].trim() == '-') {
                  isPattern = fragments[j].trim() == '+';
                } else {
                  throw new Error('unexpected fragment: ' + fragments[j]);
                }
                continue;
              }

              var fragmentParts = fragments[j].trim().split(' ');
              var nodeLabel1 = fragmentParts[0];
              var nodeLabel2 = fragmentParts[2];
              var edgeLabel = fragmentParts[3];

              // temporary, ignore UNKNOWNs
              if (nodeLabel1 == 'UNKNOWN' || nodeLabel2 == 'UNKNOWN' ) {
                // console.log('UNKNOWN found, skipping');
                continue;
              }
              if (nodeLabel1.includes('debug') || nodeLabel2.includes('debug') ) {
                // console.log('debug found, skipping');
                continue;
              }
    

              if (nodeLabel1.includes('.')) {
                nodeLabel1 = nodeLabel1.replace(/\./g, '__');
              }
              if (nodeLabel2.includes('.')) {
                nodeLabel2 = nodeLabel2.replace(/\./g, '__');
              }

              edges.push({from: nodeLabel1, to: nodeLabel2, label: edgeLabel, rawText:fragments[j]});
              if (adjlist[nodeLabel1] == undefined) {
                adjlist[nodeLabel1] = [];
              }
              if (adjlist[nodeLabel2] == undefined) {
                adjlist[nodeLabel2] = [];
              }
              adjlist[nodeLabel1].push({to: nodeLabel2, label: edgeLabel});
            }
          } else {
            var fragments = text.split(',');
            var isPattern = fragments[fragments.length - 1].trim() == '+';
            var nodeLabel1 = fragments[0].trim().split(' ')[0];
            if (nodeLabel1.includes('.')) {
              nodeLabel1 = nodeLabel1.replace(/\./g, '__');
            }
            if (nodeLabel1 == 'UNKNOWN' || nodeLabel2 == 'UNKNOWN' ) {
              console.log('UNKNOWN found, skipping');
              continue;
            }
            if (nodeLabel1.includes('debug') || nodeLabel2.includes('debug') ) {
              // console.log('debug found, skipping');
              continue;
            }
            
            adjlist[nodeLabel1] = [];
            adjlist[nodeLabel1].push({to: '', label: ''});
            edges.push({from: nodeLabel1, to: '', label: '', rawText:fragments[j]});
          }


          if (showImmediately) {

            if (i < numUserFeedback + 1) {
              Subgraphs.insert({rawText: text.replace(/\./g, '__'), edges: edges, adjlist: adjlist, discriminative:true, alternative: true, isPattern:isPattern, debug_added_from:'d', hidden: !showImmediately, labelled:false, debug_request_number: request_number});
            }
          }
          else {
            Subgraphs.insert({rawText: text, edges: edges, adjlist: adjlist, discriminative:true, alternative: true, isPattern:isPattern, debug_added_from:'d', hidden: !showImmediately, labelled:false, debug_request_number: request_number});
          }
            

            // console.log('[discriminative=true] inserted  '+ text + ' with subgraphId=' + i);
          

          
          text = '';
        }
      }
    }

    var totalDiscriminativeSubgraphs = i;
    // console.log('totalDiscriminativeSubgraphs =', totalDiscriminativeSubgraphs);

    // console.log('console.log(Subgraphs.find({discriminative:true}).count()); = '  +Subgraphs.find({discriminative:true}).count());

    spawn = Npm.require('child_process').spawn;

    focalNode = focalNode.replace('__', '.');
    // console.log('reading frequent subgraphs', 'python3', [appPath + "misc_scripts/debug_frequent_subgraphs.py", experiment_id, request_number, API, focalNode].join(' '));
    command = spawn('python3',[appPath + "misc_scripts/debug_frequent_subgraphs.py", experiment_id, request_number, API, focalNode])

    
    command.stdout.on('data',  Meteor.bindEnvironment(function (data) {
      data = data.toString();

      // console.log('[most frequent] #' + request_number + ' focalNode= ' + focalNode + '  subgraphs read: ' + data.toString());
      // var text = '';
      var toInsert = [];
      for (var i=0; i<data.split('\n').length; i++){
        var text = data.split('\n')[i];
        var edges = [];
        var adjlist = {};
        if (data.split('\n')[i].length != 0) {
          if (text.length != 0) {

            if (text.split(',').length >= 2 ) {
              var fragments = text.split(',');
              

              for (var j=0; j<fragments.length; j++) {

                if (fragments[j].trim().length == 0) {
                  continue;
                }
                var fragmentParts = fragments[j].trim().split(' ');
                var nodeLabel1 = fragmentParts[0];
                var nodeLabel2 = fragmentParts[2];
                var edgeLabel = fragmentParts[3];

                if (nodeLabel1 == 'UNKNOWN' || nodeLabel2 == 'UNKNOWN' || !nodeLabel1 || !nodeLabel2) {
                  continue;
                }
                if (nodeLabel1.includes('debug') || nodeLabel2.includes('debug') ) {
                  // console.log('debug found, skipping');
                  continue;
                }

                if (nodeLabel1.includes('.')) {
                  nodeLabel1 = nodeLabel1.replace(/\./g, '__');
                }
                if (nodeLabel2.includes('.')) {
                  nodeLabel2 = nodeLabel2.replace(/\./g, '__');
                }

                edges.push({from: nodeLabel1, to: nodeLabel2, label: edgeLabel, rawText:fragments[j]});
                if (adjlist[nodeLabel1] == undefined) {
                  adjlist[nodeLabel1] = [];
                }
                if (adjlist[nodeLabel2] == undefined) {
                  adjlist[nodeLabel2] = [];
                }
                adjlist[nodeLabel1].push({to: nodeLabel2, label: edgeLabel});
              }
            } else {
              var fragments = text.split(',');
              var isPattern = fragments[fragments.length - 1].trim() == '+';
              var nodeLabel1 = fragments[0].trim().split(',')[0];
              if (nodeLabel1.includes('.')) {
                nodeLabel1 = nodeLabel1.replace(/\./g, '__');
              }
              if (nodeLabel1 == 'UNKNOWN' || nodeLabel2 == 'UNKNOWN' ) {
                // console.log('UNKNOWN found, skipping');
                continue;
              }
              if (nodeLabel1.includes('debug') || nodeLabel2.includes('debug') ) {
                // console.log('debug found, skipping');
                continue;
              }
              
              adjlist[nodeLabel1] = [];
              adjlist[nodeLabel1].push({to: '', label: ''});
              edges.push({from: nodeLabel1, to: '', label: '', rawText:fragments[j]});
            }
            
            if (Subgraphs.find({rawText:text +',-', discriminative:true}).count() == 0 && Subgraphs.find({rawText:text +',+', discriminative:true}).count() == 0 && Subgraphs.find({rawText:text, discriminative:true}).count() == 0 ) {

              if (Subgraphs.find({rawText: text, discriminative: false}).count() == 0 && Subgraphs.find({rawText:text +',-', discriminative:false}).count() == 0 && Subgraphs.find({rawText:text +',+', discriminative:false}).count() == 0) {

                if (Subgraphs.find({discriminative:false}).count() < 3000) { // the user can't handle this many subgraphs anyway and its huge drag on performance
                  // Subgraphs.insert({rawText: text, edges: edges, adjlist: adjlist, discriminative:false, initiallyFrequent:true, debug_added_from: 'f', debug_request_number: request_number});
                  toInsert.push({rawText: text, edges: edges, adjlist: adjlist, discriminative:false, initiallyFrequent:true, debug_added_from: 'f', debug_request_number: request_number});
                }
              }
            }

            text = '';
          }
        }
      }
      

      // insert 
      if (toInsert && toInsert.length > 0) {
        Subgraphs.batchInsert(toInsert);
        // console.log('inserted ' + toInsert.length + ' subgraphs');
      }

      // console.log('console.log(Subgraphs.find({discriminative:false}).count()); = '  +Subgraphs.find({discriminative:false}).count() + ' focal: ' + focalNode);
    }));

    command.stderr.on('data', function (data) {
      console.log('stderr: [frequent] ' + data);
    });

    var totalDiscriminativeAndFrequentSubgraphs = totalDiscriminativeSubgraphs + i;
    // console.log('totalDiscriminativeAndFrequentSubgraphs =', totalDiscriminativeAndFrequentSubgraphs);

    // console.log('console.log(Subgraphs.find({}).count()); = '  +Subgraphs.find({}).count());

    var fetchAlternatives = true;

    if (fetchAlternatives) {
      spawn = Npm.require('child_process').spawn;

      // console.log('reading alternative subgraphs', 'python3',[appPath + "misc_scripts/debug_alternative_subgraphs.py", experiment_id, request_number, API].join( ' '));
      command = spawn('python3',[appPath + "misc_scripts/debug_alternative_subgraphs.py", experiment_id, request_number, API]);

      
      command.stdout.on('data',  Meteor.bindEnvironment(function (data) {
        data = data.toString();

        // console.log('[alternative] subgraphs read: ' + data.toString());
        // var text = '';
        for (var i=0; i< data.split('\n').length && i < 100; i++){ // TODO: remove the 100 limit
          var text = data.split('\n')[i];
          var edges = [];
          var adjlist = {};
          if (data.split('\n')[i].length != 0) {
            if (text.length != 0) {

              if (text.split(',').length >= 2 ) {
                var fragments = text.split(',');
                

                for (var j=0; j<fragments.length; j++) {

                  if (fragments[j].trim().length == 0) {
                    continue;
                  }
                  var fragmentParts = fragments[j].trim().split(' ');
                  var nodeLabel1 = fragmentParts[0];
                  var nodeLabel2 = fragmentParts[2];
                  var edgeLabel = fragmentParts[3];

                  if (nodeLabel1 == 'UNKNOWN' || nodeLabel2 == 'UNKNOWN' ) {
                    continue;
                  }
                  if (nodeLabel1.includes('debug') || nodeLabel2.includes('debug') ) {
                    // console.log('debug found, skipping');
                    continue;
                  }

                  if (nodeLabel1.includes('.')) {
                    nodeLabel1 = nodeLabel1.replace(/\./g, '__');
                  }
                  // console.log('fragmentParts', fragmentParts);
                  if (nodeLabel2.includes('.')) {
                    nodeLabel2 = nodeLabel2.replace(/\./g, '__');
                  }

                  edges.push({from: nodeLabel1, to: nodeLabel2, label: edgeLabel, rawText:fragments[j]});
                  if (adjlist[nodeLabel1] == undefined) {
                    adjlist[nodeLabel1] = [];
                  }
                  if (adjlist[nodeLabel2] == undefined) {
                    adjlist[nodeLabel2] = [];
                  }
                  adjlist[nodeLabel1].push({to: nodeLabel2, label: edgeLabel});
                }
              } else {
                var fragments = text.split(',');
                var isPattern = fragments[fragments.length - 1].trim() == '+';
              
                var nodeLabel1 = fragments[0].trim().split(' ')[0];
                if (nodeLabel1.includes('.')) {
                  nodeLabel1 = nodeLabel1.replace(/\./g, '__');
                }
                if (nodeLabel1 == 'UNKNOWN' || nodeLabel2 == 'UNKNOWN' ) {
                  // console.log('UNKNOWN found, skipping');
                  continue;
                }
                if (nodeLabel1.includes('debug') || nodeLabel2.includes('debug') ) {
                  // console.log('debug found, skipping');
                  continue;
                }
                
                adjlist[nodeLabel1] = [];
                adjlist[nodeLabel1].push({to: '', label: ''});
                edges.push({from: nodeLabel1, to: '', label: '', rawText:fragments[j]});
              }
              
              // if (Subgraphs.find({rawText: text, alternative: true}).count() == 0 ) {
                Subgraphs.insert({ rawText: text, edges: edges, adjlist: adjlist, alternative:true, initiallyFrequent:false, debug_added_from: 'a', debug_request_number: request_number});
                // console.log('[alternative, discriminative=false] inserted  '+ text + ' with subgraphId=' + (totalDiscriminativeAndFrequentSubgraphs+i));
              // } else { 
              //   console.log('[alternative, discriminative=false] already exists, skipping:' + text);
              // }
              
              text = '';
            }
          }
        }
        
        // console.log('console.log(Subgraphs.find({alternative:true}).count()); = '  +Subgraphs.find({alternative:true}).count());
        // console.log('console.log(Subgraphs.find({).count()); = '  +Subgraphs.find({}).count());
      }));
    }

  }));



    
  

  command.stderr.on('data', function (data) {
    console.log('stderr: ' + data);
  });

  command.on('exit', function (code) {
    console.log('child process exited with code ' + code);



  });

}

function resetDatabase() {
  experiment_id = 'test';
  request_counter = process.env.STARTING_COUNT ? parseInt(process.env.STARTING_COUNT) : 1;
  // Session.set('request_counter', 0);
  Subgraphs.remove({});
  Examples.remove({});
  TestExamples.remove({});
  Bags.remove({});
  Queries.remove({});
  Config.remove({});
  History.remove({});
  // console.log('reload',reload);
  // run script to reset graph mining data
  spawn = Npm.require('child_process').spawn;

  console.log('running ' + projectPath + "code/reset_graphs_" + 'all' + ".sh")
  command = spawn('sh', [projectPath + "code/reset_graphs_" + 'all' + ".sh"]);
  command.stdout.on('data', function (data) {
    console.log('[reset_graph.sh] stdout: ' + data);
  });

  command.stderr.on('data', function (data) {
    console.log('[reset_graph.sh] stderr: ' + data);
  });

  command.on('exit', Meteor.bindEnvironment(function (data) {
    // console.log('[reset_graph.sh] child process exited with code ' + code);
    Assets.getText('cryptoapi_bench_' + APIshortName + '.json', function (err, data) {
      var content = EJSON.parse(data);

      _.each(content, function (doc) {
        // 4 essential fields
        // url
        // rawCode
        // exampleID
        // dataset
        doc['codeLength'] = doc['rawCode'].length;

        if (!doc['label']) {
          doc['label'] = '?';
        } else {
          doc['prelabelled'] = true;
        }

        if (doc['test']) {
          TestExamples.insert(doc);
          // console.log('insert test!');
        } else {
          Examples.insert(doc);
        }

        console.log('inserted example ' + doc['exampleID']);

      });
      console.log('how many examples now?', Examples.find().count());
      Assets.getText('original_graphs/' + API + '_graph_id_mapping.txt', function (err, data) {
        if (err) {
          console.log('error when reading graph_id_mapping)=' + err);
        }
        var rows = data.split('\n');

        // iterate over all Examples
        [Examples, TestExamples].forEach(function (Examples) {
          Examples.find({}).forEach(function (example) {

            var exampleId = example.exampleID;
            var targetGraphId = -1;
            // console.log('looking for ', exampleId)
            for (let i = 0; i < rows.length; i++) {
              var line = rows[i];

              if (line.length == 0) {
                continue;
              }
              var graph_id = line.split(',')[1];
              var example_id = line.split(',')[2].split(' - ')[0];
              // var methodName = line.split(',')[2].split(' - ')[1].split('.')[1].split('#')[0];
              var lines = _.filter(example.rawCode.split(' '), function (item) { return item.includes('('); });
              for (let j = 0; j < lines.length; j++) {
                var codeline = lines[j];
                var methodName = codeline.split('(')[0];

                // console.log('exampleid,', example_id)
                // console.log('methodName,', methodName)
                // console.log('line,', line)
                if (line.split(',')[2].split(' - ')[1].includes(methodName) && example_id == exampleId) {
                  // console.log('found graph_id ' + graph_id + ' for example ' + exampleId + ' with method ' + methodName);
                  targetGraphId = graph_id;
                  break;
                }
              }
            }


            if (targetGraphId != -1) {
              console.log('[matching example to graph] targetGraphId = ' + targetGraphId + ' for example ' + exampleId + ' of dataset ' + example.dataset);

              var a = Examples.update({ exampleID: parseInt(exampleId) }, { $set: { graphId: parseInt(targetGraphId) } }, function (error, result) {
                if (error)
                  console.log('error when updating Examples with graphid---->' + error);
                // console.log('done updating graphid for example ' + exampleId + ' to ' + graph_id)
              });
              var a = TestExamples.update({ exampleID: parseInt(exampleId) }, { $set: { graphId: parseInt(targetGraphId) } }, function (error, result) {
                if (error)
                  console.log('error when updating Examples with graphid---->' + error);
                // console.log('done updating graphid for example ' + exampleId + ' to ' + graph_id)
              });

            }
          });
        });
        // console.log(Examples.findOne()); 
        // load 
        Assets.getText('original_graphs/' + API + '_elementpositions.json', function (err, data) {
          if (err) {
            console.log('error when reading program element positions =' + err);
          }

          var element_position = EJSON.parse(data);
          console.log('element_position', Object.keys(element_position).length);

          // iterate over keys of element_position
          for (let i = 0; i < Object.keys(element_position).length; i++) {
            var key = Object.keys(element_position)[i];
            // console.log('reading element positions of graph ' + key + '...');
            var one_element_position = EJSON.parse(element_position[key]);
            var expressionStarts = one_element_position['expressionStart'];
            var expressionEnds = one_element_position['expressionEnd'];
            var rawCode = one_element_position['rawCode'];

            var programElementToExpression = {};

            _.each(expressionStarts, function (start, startkey) {
              var end = expressionEnds[startkey];

              if (startkey.includes('.')) {
                startkey = startkey.replace(/\./g, '__');
              }
              programElementToExpression[startkey] = {};
              programElementToExpression[startkey]['expressionStart'] = start;
              programElementToExpression[startkey]['expressionEnd'] = end;
            });
            console.log('setting', Object.keys(expressionStarts).length, 'element positions of graph ' + key + '...');
            programElementToExpression['codeElements'] = Object.keys(expressionStarts);
            programElementToExpression['rawCode'] = rawCode;

            // Examples.find({graphId: parseInt(key)}).forEach(function (example) {
            // console.log('setting element positions of graph ' + key + '...' + 'with keys ' + Object.keys(programElementToExpression));
            [Examples, TestExamples].forEach(function (Examples) {
              Examples.update({ graphId: parseInt(key) }, { $set: programElementToExpression }, function (error, result) {
                if (error)
                  console.log('error when updating Examples with programElementToExpression---->' + error);
              });
            });

          }
        });

      });

    });

  }));
}

function greedySetCover(subsets, parent_set, max_size) {
  parent_set = new Set(parent_set);
  let results = [];
  let result_set = new Set();
  let prev_size = -1;

  while (result_set.size < parent_set.size && prev_size < result_set.size && results.length < max_size && subsets.size > 0) {
    // console.log('greedySetCover: result_set.size = ', result_set.size, ' parent_set.size = ', parent_set.size, ' prev_size = ', prev_size);
    prev_size = result_set.size;
    // let [selected_id, add_set] = [...subsets.entries()].reduce((a, b) => (new Set([...a].filter(x => !result_set.has(x))).size > new Set([...b].filter(x => !result_set.has(x))).size) ? a : b);

    var add_set = new Set();
    var selected_id;
    for (let key of subsets.keys()) {

      let subset = subsets.get(key);
      var addition = new Set([...subset].filter(x => !result_set.has(x)));
      // console.log('greedySetCover[in loop]: subset = ', subset, ' result_set = ', result_set, ' addition = ', addition);
      if (addition.size > add_set.size) {
        add_set = addition;
        selected_id = key;
      }
      // console.log('greedySetCover[in loop]:', key, addition.size, add_set.size )
    }

    results.push(selected_id);
    add_set.forEach(x => result_set.add(x));
    subsets.delete(selected_id);
    // console.log('[end iteration] greedySetCover: result_set.size = ', result_set.size, ' parent_set.size = ', parent_set.size, ' prev_size = ', prev_size, ' results.length = ', results.length, 'selected_id', selected_id);
  }
  // what's not covered?
  let uncovered = new Set(parent_set);
  result_set.forEach(x => uncovered.delete(x));
  // console.log('[end greedySetCover] result_set.size = ', result_set.size, ' parent_set.size = ', parent_set.size, ' prev_size = ', prev_size, ' results.length = ', results.length);
  // console.log('[greedySetCover] uncovered = ', uncovered.size);

  return [results, result_set];
} 


var shellOutToComputeQueryExamples = function(experiment_id, request_number, API) {
  
  const dummy = false; // just randomly select examples if dummy
  var noQuerySelector = {'$and': [ {label: {$ne: true}}, {query: {'$ne': true}}]};
  var selector = constructSelectorToFilterBaggedPatterns(noQuerySelector);
  // console.log(JSON.stringify(selector));
  var examples = Examples.find(selector).fetch();
  console.log('[shellOutToComputeQueryExamples] size: ' + examples.length);
  if (dummy) {
    var queryExamples = examples.slice(0, 3);

    queryExamples.forEach(function(queryExample) {
      Examples.update({_id: queryExample._id}, {$set: {query: true}});
    });

    console.log('queryExamples = ', queryExamples);
  } else {
    // shell out
    // spawn = Npm.require('child_process').spawn;
    // write to file, all examples and the codeElements
    
    var examplesToElements = new Map();
    var allCodeElements = new Set();

    var unlabelledExamples = examples.filter(function(example) { return example.label == '?'; });
    // console.log(unlabelledExamples);
    // var codeLengthThreshold = unlabelledExamples
    // .reduce((a, b) => a + b.codeLength, 0) / unlabelledExamples.length / 2;
    var codeLengthThreshold = 1600;
    unlabelledExamples.forEach(function(example) {

      // console.log(example)
      // consider only shorter examples
      if (example.codeLength <= codeLengthThreshold) {
        examplesToElements.set(example.exampleID, new Set(example.codeElements));
      }
      example.codeElements.forEach(function(codeElement) {
        allCodeElements.add(codeElement);
      }
      );
    });
    console.log('[shellOutToComputeQueryExamples] examplesToElements.size = ', examplesToElements.size, ' allCodeElements.size = ', allCodeElements.size, 'codeLengthThreshold=', codeLengthThreshold);

    var [queryExamplesIDs, result_set] = greedySetCover(examplesToElements, allCodeElements, 5);
    // console.log('queryExamplesIDs = ', queryExamplesIDs);

    queryExamplesIDs.forEach(function(queryExampleID) {
      Examples.update({exampleID: queryExampleID}, {$set: {query: true}});
    });
    console.log('queryExamplesIDs = ', queryExamplesIDs);

  }
}

function computeSelector(subgraphs) {
  var selector = {};

  var nodes = {};
  var negateMatches = {}
  subgraphs.forEach(function(subgraph) {
    
    subgraph.edges.forEach(function(edge) {
      nodes[edge.from] = true;

      negateMatches[edge.from] = !_.isUndefined(subgraph.isPattern) && !_.isNull(subgraph.isPattern)  &&  !subgraph.isPattern;

      if (edge.to == '') return;
      nodes[edge.to] = true;

      negateMatches[edge.to] = !_.isUndefined(subgraph.isPattern) && !_.isNull(subgraph.isPattern)  &&  !subgraph.isPattern;
    });
  });

  var newConjunctions = 
    Object.keys(nodes)
    .filter(filterNodeLabel)
    .map(function(nodeLabel){
      var obj = {};
      if (nodeLabel.includes('.')) {
        nodeLabel = nodeLabel.replace(/\./g, '__');
      }
      
      if (negateMatches[nodeLabel]) {
        obj[nodeLabel] = {$exists: false};
      } else {
        obj[nodeLabel] = {$exists: true};
      }
     
      return obj;
    });
    if (selector['$and']) {
      selector['$and'] = selector['$and'].concat(newConjunctions);
    } else {
      selector['$and'] = newConjunctions;
    }

  return selector;
}

var filterNodeLabel = function(nodeLabel) {
  return nodeLabel != 'UNKNOWN' && nodeLabel != '<a>' && nodeLabel != '<r>' && !nodeLabel.includes("pseudo");
}

var constructSelectorToFilterBaggedPatterns = function(selector) {
  var disjunct = [];

  var bagsToSubgraphs = {};
  Subgraphs.find({ 'bags': { '$exists': true, '$ne': [] } }).forEach(function (subgraph) {
    var subgraphID = subgraph._id;
    var subgraphBag = subgraph.bags;

    if (bagsToSubgraphs[subgraphBag] == undefined) {
      bagsToSubgraphs[subgraphBag] = [];
    }
    bagsToSubgraphs[subgraphBag].push(subgraphID);
  });

  Object.keys(bagsToSubgraphs) // iterate over the bags
    .forEach(function (bag) {
      var subgraphIds = bagsToSubgraphs[bag];
      var subgraphs = Subgraphs.find({ _id: { '$in': subgraphIds } }).fetch();
      var subgraphsSelector = computeSelector(subgraphs);
      disjunct.push(subgraphsSelector);
    });

  if (selector['$and'] != undefined) {
    if (disjunct.length > 0) {
      selector['$and'].push({ '$nor': disjunct });
    }
  } else {
    if (disjunct.length > 0) {
      selector['$and'] = [{ '$nor': disjunct }];
    }
  }
  return selector;
}

function computeSelectorFromSkeleton(skeleton) {
  if (skeleton == undefined || Object.keys(skeleton).length == 0) {
    return {};
  }
  // console.log(skeleton);

  // from the skeleton, obtain the nodes that are checked
  var selector = {};
  var subgraphNodeLabels = 
    Object.keys(skeleton)
    .filter(function(nodeLabel) {
      var checked = skeleton[nodeLabel].checked;
      return checked;
    })
    .map(function(nodeLabel) {
      return nodeLabel;
    });
  var newConjunctions = 
    subgraphNodeLabels.map(function(nodeLabel){
      var obj = {};

      var skeletonPart = skeleton[nodeLabel];
      var negateMatch = skeletonPart.matchStatus == 'misuse';

        
      if (nodeLabel.includes('.')) {
        nodeLabel = nodeLabel.replace(/\./g, '__');
      }
      // console.log('building new conjunctions', nodeLabel);
      if (!negateMatch) {
        obj[nodeLabel] = {$exists: true};
      } else {
        obj[nodeLabel] = {$exists: false};
      }
      
      return obj;
    });
  // console.log('newConjunctions');
  // console.log(Object.keys(newConjunctions));
    
  if (selector['$and']) {
    selector['$and'] = selector['$and'].concat(newConjunctions);
  } else {
    selector['$and'] = newConjunctions;
  }

  
  if (selector['$and'] && selector['$and'].length == 0) {
    delete selector['$and'] ;
  }
  return selector;
}


var buildSkeleton = function(){
  
  // if (Session.get('skeleton') && Object.keys(Session.get('skeleton')).length > 0) {
  //   return;
  // }

  var nodeContainedInSubgraph = {};
  
  var nodeContainedInCorrectSubgraph = {};
  var nodeContainedInMisuseSubgraph = {};

  var nodes = {};
  Subgraphs.find( { '$or' : [{discriminative: true}] }).forEach(function(subgraph) {
    
    subgraph.edges.forEach(function(edge) {
      
        
      if (filterNodeLabel(edge.from)) {
        nodes[edge.from] = true;

        if (!nodeContainedInSubgraph[edge.from]) {
          nodeContainedInSubgraph[edge.from] = [];
        }
        nodeContainedInSubgraph[edge.from].push(subgraph._id);
        if (subgraph.labelled) {
          // console.log('subgraph labelled for ed.gefrom-> ' + edge.from);
          if (subgraph.isPattern) {
            if (!nodeContainedInCorrectSubgraph[edge.from]) {
              nodeContainedInCorrectSubgraph[edge.from] = [];
            }
            nodeContainedInCorrectSubgraph[edge.from].push(subgraph._id);
          } else {
            if (!nodeContainedInMisuseSubgraph[edge.from]) {
              nodeContainedInMisuseSubgraph[edge.from] = [];
            }
            nodeContainedInMisuseSubgraph[edge.from].push(subgraph._id);
          }
        }
      }

      if (filterNodeLabel(edge.to) && edge.to != '') {
        nodes[edge.to] = true;
        if (!nodeContainedInSubgraph[edge.to]) {
          nodeContainedInSubgraph[edge.to] = [];
        }

        nodeContainedInSubgraph[edge.to].push(subgraph._id);

        if (subgraph.labelled) {
          // console.log('subgraph labelled for ed.geto -> ' + edge.to);
          if (subgraph.isPattern) {
            if (!nodeContainedInCorrectSubgraph[edge.to]) {
              nodeContainedInCorrectSubgraph[edge.to] = [];
            }

            nodeContainedInCorrectSubgraph[edge.to].push(subgraph._id);
          } else {
            if (!nodeContainedInMisuseSubgraph[edge.to]) {
              nodeContainedInMisuseSubgraph[edge.to] = [];
            }
            nodeContainedInMisuseSubgraph[edge.to].push(subgraph._id);
          }
        }
      }
    });
  });

  // console.log('nodeContainedInCorrectSubgraph');
  // console.log(nodeContainedInCorrectSubgraph);
  // console.log('nodeContainedInMisuseSubgraph');
  // console.log(nodeContainedInMisuseSubgraph);
  var skeleton = {};  

  Object.keys(nodes).forEach(function(nodeLabel){
    var subgraphIds = [];
    if (nodeContainedInCorrectSubgraph[nodeLabel]) {
      subgraphIds.push( ...nodeContainedInCorrectSubgraph[nodeLabel]);
    }
    if (nodeContainedInMisuseSubgraph[nodeLabel]) {
      subgraphIds.push( ...nodeContainedInMisuseSubgraph[nodeLabel]);
    }


    var matchStatus;
    if (nodeContainedInCorrectSubgraph[nodeLabel] && nodeContainedInCorrectSubgraph[nodeLabel].length > 0) {
      if ( nodeContainedInMisuseSubgraph[nodeLabel] && nodeContainedInMisuseSubgraph[nodeLabel].length > 0) {
        matchStatus = 'mixed';
      } else {
        matchStatus = 'correct';
      }
      
    } else if (nodeContainedInMisuseSubgraph[nodeLabel] && nodeContainedInMisuseSubgraph[nodeLabel].length > 0) {
      if ( nodeContainedInCorrectSubgraph[nodeLabel] && nodeContainedInCorrectSubgraph[nodeLabel].length > 0) {
        matchStatus = 'mixed';
      }
      else {
        matchStatus = 'misuse';
      }

    }
   
    skeleton[nodeLabel] = {
      'text': nodeLabel.replace(/\./g, '__'),
      'checked': matchStatus === 'correct' || matchStatus === 'misuse', 
      'matchStatus': matchStatus,
      'subgraphIds': subgraphIds,
      'allSubgraphs': nodeContainedInSubgraph[nodeLabel]
    };
  });
  return skeleton;

}

var filterByViewAndKeyword = function(skeleton, viewType, keyword){
  // account for the view type
  
  if (viewType === 'all' || !viewType) {
    // default behavior
    var selector = constructSelectorToFilterBaggedPatterns({});
    
  } else if (viewType === 'matching') {
    if (_.isEmpty(skeleton)){
      return {};
    }
    // apply the skeleton selector
    var selector = computeSelectorFromSkeleton(skeleton);
    selector = constructSelectorToFilterBaggedPatterns(selector);
    
  } else if (viewType === 'unlabelled') {
    // select examples not labelled
    var selector = {'label': {'$nin': [ 'positive', 'negative']}};
    selector = constructSelectorToFilterBaggedPatterns(selector);
    
    
  } else if (viewType === 'labelled') {
    // select examples not labelled
    var selector = {'label': {'$in': [ 'positive', 'negative']}};
    selector = constructSelectorToFilterBaggedPatterns(selector);
    
    
    
  }  else if (viewType === 'confused') {
    if (_.isEmpty(skeleton)){
      return {};
    }
    
    // select examples labelled, but mismatching the skeleton
    // 1. select examples matching the skeleton, but are negative
    // 2. select examples not matching the skeleton, but are positive
    var selector = computeSelectorFromSkeleton(skeleton);
    selector['$and'] = [{'label': 'negative'}];

    selector = constructSelectorToFilterBaggedPatterns(selector);


  } else if ( viewType === 'not-matching') {
    if (_.isEmpty(skeleton)){
      return {};
    }
    // select examples not matching the skeleton
    var selector = {'$and': [{'label': 'positive'}, {'$nor': computeSelectorFromSkeleton(skeleton)['$and']}]};
    selector = constructSelectorToFilterBaggedPatterns(selector);
    
  }

  if (keyword) {  
    const regex = new RegExp(keyword, 'i');
    const query = { codeElements: regex };

    if (!selector['$and']){
      selector['$and'] = [];
    }
    selector['$and'].push(query);
  }

  return selector;
}

function escapeNodeForDisplay(text) {
  if (text == undefined) return '';
  console.log('text', text);

  var result = text.replace('__', '.')
    // .replace("UNKNOWN", "<expr>");
    .replace("UNKNOWN.", "");
  if (result.includes("String:")) {
    result = '' + result.replace("String:", '"') + '"';
  }
  if (result.includes("int:")) {
    result = '' + result.replace("int:", '') + '';
  }

  if (result.includes("<catch>")) {
    result = "catch (...) ";
  }
  if (result.includes("<r>")) {
    result = "if (...) ";
  }
  if (result.includes('<throw>')) {
    result = 'throw ';
  }
  if (result.includes("<init>")) {
    result = '<span class="hljs-keyword">' +  "new " + '</span>' + result.replace(".<init>", "(...)");
  }
  if (result.includes("<cast>")) {
    result = "(" + result.replace(".<cast>", ") ...");
  }
  if (result.includes("<return>")) {
    result = "return ";
  }
  if (result.includes("<break>")) {
    result = "break ";
  }
  if (result.includes("<continue>")) {
    result = "continue ";
  }
  if (result.includes('<nullcheck>')) {
    result = ' == null';
  }
  if (result.includes("<instanceof>")) {
    result = '... <span class="hljs-keyword">instanceof </span>' + result.replace(".<instanceof>", "") + '';
  }
  return result;
}


var getSmplMixup = function(API) {
  // get the current skeleton
  // convert to spatch

  var subgraphAsSmpl = Subgraphs.find( { '$or' : [{discriminative: true}] }).map(function(subgraph) {
    var text = '';
    subgraph.edges.forEach(function(edge) {
      if (edge.to == '') return;

      if (edge.label.includes('para')) {
        if (edge.from.includes('(')) {
          // text += escapeNodeForDisplay(edge.from).replace('(','').replace(')','') + '(' + escapeNodeForDisplay(edge.to) + ') ';
          text += '* ' + escapeNodeForDisplay(edge.from).replace('(','').replace(')','') + '(C) ';
        } else {
          // text += escapeNodeForDisplay(edge.to).replace('(','').replace(')','') + '(' + escapeNodeForDisplay(edge.from) + ')';
          text += '* ' + escapeNodeForDisplay(edge.to).replace('(','').replace(')','') + '(C)';
          
        }

      } else if (edge.label.includes('order')) {
        text += escapeNodeForDisplay(edge.from) + '\n...\n' + escapeNodeForDisplay(edge.to) + '';
      }
      
    });
    return text;
  }).join('\n');


  
  console.log('subgraphAsSmpl');
  console.log(subgraphAsSmpl);
  command = spawn('python3',[appPath + "transform_programs.py", subgraphAsSmpl, API]);

  command.stdout.on('data',  function (data) {
    console.log('[getSmplMixup] stdout: ' + data);
  });

  command.stderr.on('data', function (data) {
      console.log('[getSmplMixup] stderr: ' + data);
  });

  command.on('exit', Meteor.bindEnvironment(function (code) {
    console.log('child process exited with code ' + code);
    // read the output file

    var example = fs.readFileSync(appPath.replace('meteor_app', 'graphs') + 'smpl_output.txt', 'utf8');

    Queries.insert({
      'rawCode': example,
    });
  }));
}

var fs = Npm.require('fs');



var getOpenaiCompletion = function(API) {

  // get the current skeleton

  var subgraphAsText = Subgraphs.find( { '$or' : [{discriminative: true}] }).map(function(subgraph) {
    var text = '';
    subgraph.edges.forEach(function(edge) {
      if (edge.to == '') return;
      text += edge.from + ' -' + edge.label + '-> ' + edge.to + ', ';
    });
    return text;
  }).join('\n');
  

  const completion = openai.createCompletion({
    model: "text-davinci-003",
    prompt: "Construct an example of using  " + API + " in Java. Ensure that the example usage shows only a single, self-contained method (i.e., no class), and does not suffer from bugs or vulnerabilities. Start your response with 'public static void '. We will like to find usages containing the following code elements:" + subgraphAsText,
    max_tokens: 500,
  });
  completion.then(function(result) {
    
    console.log("openai result")
    // console.log(result);
    console.log(result.data.choices[0].text);

    var example = result.data.choices[0].text;
    Queries.insert({
      'rawCode': example,
    });
      

  }).catch(function(err) {
    console.error(err);
  });

  // const completion2 = openai.createCompletion({
  //   model: "text-davinci-003",
  //   prompt: "Construct an example of using  " + API + " in Java. Ensure that the example usage shows only a single, self-contained method (i.e., no class), BUT suffers from bugs or vulnerabilities. Respond with ONLY code. Start your response with 'public static void ' ",
  //   max_tokens: 500,
  // });
  // completion2.then(function(result) {
    
  //   console.log("openai result")
  //   console.log(result);
  //   console.log(result.data.choices[0].text);

  //   var example = result.data.choices[0].text;
  //   Queries.insert({
  //     'rawCode': example,
  //   });
      

  // }).catch(function(err) {
  //   console.error(err);
  // });
}


var shellOutToMineSubgraphs = function(graphId, labels, elementIdToGraphId, focalNode, nodesToInclude, eraseOld, showImmediately) {
  shellOutToMineSubgraphsMultiple([graphId], labels, elementIdToGraphId, focalNode, nodesToInclude, eraseOld, showImmediately);
}

var shellOutToMineSubgraphsMultiple = function(graphIds, labels, elementIdToGraphId, focalNode, nodesToInclude, eraseOld, showImmediately) {
  
  spawn = Npm.require('child_process').spawn;

  // this elementIdToGraphId map controls which examples are inspected by the subgraph miner
  const encoded = Buffer.from(JSON.stringify(elementIdToGraphId)).toString('base64');

  var request_number = request_counter;
  // this also runs the code that mines the subgraphs
  
  // console.log('spawining child process for mining subgraphs. target graph ids', graphIds, labels);
  
  // join graphIds by comma
  var joinedGraphId = graphIds.join(',');
  nodesToInclude = nodesToInclude.join(',');
  // console.log('spawnin...' + 'python3',[appPath + "update_labels.py", joinedGraphId, labels, encoded, experiment_id, request_counter, API, focalNode, nodesToInclude]);
  command = spawn('python3',[appPath + "update_labels.py", joinedGraphId, labels, encoded, experiment_id, request_counter, API, focalNode, nodesToInclude]);

  // console.log('request_counter = ' + request_counter);
  // Session.set('request_counter', request_counter + 1);
  request_counter += 1;

  command.stdout.on('data',  function (data) {
    // console.log('[shellOutToMineSubgraphsMultiple] stdout: ' + data);
  });

  command.stderr.on('data', function (data) {
      // console.log('[shellOutToMineSubgraphsMultiple] stderr: ' + data);
  });

  command.on('exit', Meteor.bindEnvironment(function (code) {
    // console.log('child process exited with code ' + code);
    shellOutToReadSubgraphs(request_number, focalNode, eraseOld, showImmediately);

    // shellOutToMinePatternsMultiple(graphIds, labels, elementIdToGraphId);
  }));
  // shellOutToMinePatternsMultiple(graphIds, labels, elementIdToGraphId);
}




var shellOutToCreateNewExample = function(text, label, dataset, view, keyword) {
  // write text to file 
  console.log('=====');
  console.log(text);
  console.log('=====');

  var fullProgram = `public class NewExampleUse {`
  fullProgram += text;
  fullProgram += `\n}`;

  // write to file
  
  fs.writeFile(projectPath + '/code/graphs/newJavaProgram.java', fullProgram, function(err) {
    if (err) {
      return console.log(err);
    } else {
      console.log("The file was saved!");
    }
  });


  var miningLabel = label === 'positive' ? '+' : '-';

  spawn = Npm.require('child_process').spawn;
  console.log('spawining child process for creating new example', text, miningLabel);

  command = spawn('java',[ '-jar', appPath + "misc_scripts/graph_convertor.jar" ,
    API, projectPath + "/code/graphs/", projectPath + "/code/graphs/", label]);
  console.log('command = ' + command + ' ' + API + ' ' + projectPath + "/code/graphs/" + ' ' + projectPath + "/code/graphs/" + ' ' + label);

  command.stdout.on('data',  function (data) {
      console.log('[shellOutToCreateNewExample stdout: ' + data);
  });
  command.stderr.on('data', function (data) {
    console.log('[shellOutToCreateNewExample] stderr: ' + data);
  });

  command.on('exit', Meteor.bindEnvironment(function (code) {
    console.log('child process exited with code ' + code);

    // read the file,projectPath +  /code/graphs/java.security.MessageDigest__digest_test_formatted.txt, and find largest graph id
    var graphId = 0;
    var file = fs.readFileSync(projectPath + '/code/graphs/' + API + '_test_formatted.txt', 'utf8');
    var lines = file.split('\n');
    for (var i = 0; i < lines.length; i++) {
      var line = lines[i];
      if (line.startsWith('t')) {
        graphId += 1;;
      }
    }
    graphId += 1;

    // next, read java.security.MessageDigest__digest_test_elementpositions.json
    var elementPositions = JSON.parse(fs.readFileSync(projectPath + '/code/graphs/' + API + '_test_elementpositions.json', 'utf8'));
    
    var key = Object.keys(elementPositions)[0];

    var one_element_position = EJSON.parse(elementPositions[key]);

    var expressionStarts = one_element_position['expressionStart'];
    var expressionEnds = one_element_position['expressionEnd'];

    var programElementToExpression = {};
  
    _.each(expressionStarts, function(start, startkey){
      var end = expressionEnds[startkey];

      if (startkey.includes('.')) {
        startkey = startkey.replace(/\./g, '__');
      }
      programElementToExpression[startkey] = {};
      programElementToExpression[startkey]['expressionStart'] = start;
      programElementToExpression[startkey]['expressionEnd'] = end;
    });
    programElementToExpression['codeElements'] = Object.keys(expressionStarts);
    
    
    // update the database
    var newExampleId = Examples.find().count() + 1;
    var newExample = {
      exampleID: newExampleId,
      text: text,
      label: label,
      dataset: dataset,
      graphId: graphId,
      codeElements : Object.keys(expressionStarts),
      rawCode : text
    };
    // extend newExample with programElementToExpression
    newExample = _.extend(newExample, programElementToExpression);
    console.log('newExample = ' + JSON.stringify(newExample));
    
    Examples.insert(newExample, function (error, result) {
      console.log('error when inserting new example---->' + error);
    });


    // trigger code to mine subgraphs
    var elementIdToGraphId = {};
    // exclude the examples that do not match the current view
    var selector = filterByViewAndKeyword(buildSkeleton(), view, keyword);
    Examples.find(selector).forEach(function (example) {
      elementIdToGraphId[example.exampleID] = example.graphId;
    });

    // graphId, labels, elementIdToGraphId, focalNode, nodesToInclude, eraseOld, showImmediately
    // shellOutToMineSubgraphs(-1, null, elementIdToGraphId, false, "---dummy---", false);
    shellOutToMineSubgraphs(-1, label, elementIdToGraphId, "---dummy----", [], false, false);

    }
  ));
}

var shellOutToCreateNewExampleFromRepo = function(path, dataset, view, keyword) {

  spawn = Npm.require('child_process').spawn;
  if (path.includes('github.com')) {
    // clone the repository into a temporary directory
    var tempDir = projectPath + '/code/temp/';
    var tempPath = tempDir + path.split('/').pop();
    console.log('tempPath = ' + tempPath);
    var command = spawn('git', ['clone', path, tempPath]);
    command.stdout.on('data',  function (data) {
        console.log('[shellOutToCreateNewExampleFromRepo stdout: ' + data);
    });
    command.stderr.on('data', function (data) {
      console.log('[shellOutToCreateNewExampleFromRepo] stderr: ' + data);
    });
    command.on('exit', Meteor.bindEnvironment(function (code) {
      console.log('child process exited with code ' + code);
      shellOutToCreateNewExampleFromRepo(tempPath, dataset, view, keyword);
    }));

    return;
  }


  
  console.log('spawining child process for applying patterns on new examples', [ '-jar', appPath + "misc_scripts/multiple_graph_convertor.jar" ,
  API, path, projectPath + "/code/graphs/", ]);

  var command = spawn('java',[ '-jar', appPath + "misc_scripts/multiple_graph_convertor.jar" ,
    API, path, projectPath + "/code/graphs/", ]);


  command.stdout.on('data',  function (data) {
    console.log('stdout: ' + data);
  });
  command.stderr.on('data', function (data) {
    console.log('stderr: ' + data);
  });

  command.on('exit', Meteor.bindEnvironment(function (code) {

    try {
    console.log('child process exited with code ' + code);

    // read the file,projectPath +  /code/graphs/java.security.MessageDigest__digest_test_formatted.txt, and find largest graph id
    var graphId = 0;

    // looks like we're be succesful.
    // clear old examples
    Examples.remove({dataset: dataset});
    request_counter = 1;
    // clear non discriminative subgraphs
    var a = Subgraphs.remove({'$and': [{'discriminative': {'$ne': true}}, { 'labelled': {'$ne': true} }, { '$or': [ {'bag' : {'$exists': false}}, {'bag': {$eq: null}}  ]}]});
    

    console.log(projectPath + '/code/graphs/' + API + '_elementpositions.json')
    var elementPositions = JSON.parse(fs.readFileSync(projectPath + '/code/graphs/' + API + '_elementpositions.json', 'utf8'));
    // Object.keys(elementPositions).forEach(function(key){
      // for loop to iterate the keys of ElementPositions
    for (var i = 0; i < Object.keys(elementPositions).length; i++) {
      var key = Object.keys(elementPositions)[i];
      console.log('processing', key, elementPositions[key]);
    
      var one_element_position = EJSON.parse(elementPositions[key]);

      var expressionStarts = one_element_position['expressionStart'];
      var expressionEnds = one_element_position['expressionEnd'];

      var programElementToExpression = {};
    
      _.each(expressionStarts, function(start, startkey){
        var end = expressionEnds[startkey];

        if (startkey.includes('.')) {
          startkey = startkey.replace(/\./g, '__');
        }
        programElementToExpression[startkey] = {};
        programElementToExpression[startkey]['expressionStart'] = start;
        programElementToExpression[startkey]['expressionEnd'] = end;
      });
      programElementToExpression['codeElements'] = Object.keys(expressionStarts);
      
    
      // update the database
      graphId += 1;
      var newExampleId = i;
      var newExample = {
        exampleID: newExampleId,
        text: one_element_position['rawCode'],
        dataset: dataset,
        graphId: graphId,
        codeElements : Object.keys(expressionStarts),
        rawCode : one_element_position['rawCode']
      };
      // extend newExample with programElementToExpression
      newExample = _.extend(newExample, programElementToExpression);
      console.log('newExample = ' + newExampleId + ' : ' + JSON.stringify(newExample));
      
      Examples.insert(newExample, function (error, result) {
        console.log('error when inserting new example---->' + error);
      });
    }


    // trigger code to mine subgraphs
    var elementIdToGraphId = {};
    // exclude the examples that do not match the current view
    var selector = filterByViewAndKeyword(buildSkeleton(), view, keyword);
    Examples.find(selector).forEach(function (example) {
      elementIdToGraphId[example.exampleID] = example.graphId;
    });

    shellOutToMineSubgraphs(-1, null, elementIdToGraphId, "---dummy----", [], false, false);
  } catch (e) {
    console.log('error caught! when connecting to repo', e);

  }
    }
  ));
}


// var shellOutToSelectClusters = function (

Meteor.methods({
  'createNewExample'({text, label, dataset, view, keyword}) {

    shellOutToCreateNewExample(text, label, dataset, view, keyword);
  },
  'connectToRepo'({path, dataset, view, keyword}) {
    try{
      console.log('connecting to repo', path);
    shellOutToCreateNewExampleFromRepo(path, dataset, view, keyword);
    console.log('[done] connecting to repo', path);
    } catch (e) {
      console.log('error when connecting to repo', e);
    }
  },
  'updateLabels'({ exampleId, methodName, labels, view, keyword, focalNode }) {
    console.log('got the labels ' + exampleId + '  ' + methodName + ' ' + labels + ' ' + view + ' keyword:' + keyword + 'focalNode = ' + focalNode);
    

    var a = Examples.update({exampleID: parseInt(exampleId)}, {$set: {label: labels}}, function (error, result) {
        if (error) {
          console.log('error when updating Examples with label---->' + error);
        } else {
          console.log('updated Examples with label---->' + result);
        }
    });

    Subgraphs.find({discriminative:true, hidden:true, labelled:false}).forEach(function(subgraph) {
      console.log('[discriminative=true] removing subgraph: ' + subgraph.rawText);
      Subgraphs.remove({_id: subgraph._id});
    });

    var targetGraphId = -1;
    // given the exampleId and methodName, match the graph id based on java.security.MessageDigest__digest_graph_id_mapping.txt
    // the `java.security.MessageDigest__digest` files came from manually running scripts in the src2egroum2aug (HJGraphBuilderForActiveLearningInterface) project in eclipse
    
    var elementIdToGraphId = {};
    // exclude the examples that do not match the current view
    var selector = filterByViewAndKeyword(buildSkeleton(), view, keyword);
    console.log('updating labels, selector is ', selector, 'view is ', view)
    console.log('count is ', Examples.find(selector).count());
    
    Examples.find(selector).forEach(function (example) {
      elementIdToGraphId[example.exampleID] = example.graphId;
    });

    if (elementIdToGraphId.hasOwnProperty(exampleId)) {
        targetGraphId = elementIdToGraphId[exampleId];
    } else {
        targetGraphId = -1;
    }
    var eraseOld = false;
    if (focalNode == null) {
      focalNode = "---dummy---";
      eraseOld = true;
    }
    console.log(' [updateLabels] ' + 'focalNode '  + focalNode);
    shellOutToMineSubgraphs(targetGraphId, labels, elementIdToGraphId,  focalNode, [], eraseOld, false);
  },
 

  'resetLabels'({ exampleId, methodName, labels, view, keyword, focalNode }) {
    console.log('reset labels');

    Examples.update({}, {$set: {label: '?'}} , {multi: true}, function (error, result) {
      if (!error) {
        console.log('reset labels done without error '  + result);
      } else {
        console.log('error when updating Examples with label---->' + error);
      }
      console.log(Examples.find({}).fetch().map(example => example.label));
    });
    Subgraphs.update({hint: true} , {$set: {hint: null}}, {multi: true}, function (error, result) {
      if (!error) {
        console.log('reset labels done without error '  + result);
      } else {
        console.log('error when updating Examples with label---->' + error);
      }
      console.log(Subgraphs.find({}).fetch().map(example => example.hint));
    });
    
  },

  'inferPatterns'() {
    var subgraph = null;
    
    Subgraphs.find({discriminative: true, hidden: true}).fetch().forEach(function (oneSubgraph) {

      // console.log('infer patterns! Subgraphs found = ' + JSON.stringify(oneSubgraph));
      var node1 = oneSubgraph.rawText.split(' -> ')[0];
      var node2 = oneSubgraph.rawText.split(' -> ')[1].split(' ')[0];
      if ( 
        oneSubgraph.isPattern)
        // (node1.includes('(') || node2.includes('(') || node1.includes('init') || node2.includes('init'))) { // favour positive patterns and method calls
        // avoid non-methods, non-identifiers
        if (node1.length <= 3 || node2.length <= 3) {
          return
        }
        if (!subgraph) {
          subgraph = oneSubgraph;
        }
      // }
    });
    
    // console.log('infer patterns! Subgraphs found = ' + JSON.stringify(Subgraphs.find({discriminative: true, hidden: true}).fetch()));

    if (!subgraph) {
      console.log('not suitable pattern found...')
      return;
    }
    Subgraphs.update({_id: subgraph._id}, {$set: {hidden: false, labelled: true, isPattern: true}});

    // create new nodes for each edge's source and target
    var nodes = [];
    
    // console.log('infer patterns:: one subgraph  = ' + subgraph.rawText);

    var edges = subgraph.edges;
    edges.forEach(function (edge) {
      var source = edge.from;
      var target = edge.to;
      
      // console.log('infer patterns:: one subgraph edge = ' + edge.rawText);


      var adjlist = {};
      var edges = [];
      if (source) {
        source = source.replace(/\./g, '__');
        adjlist[source] = [];
        adjlist[source].push({to: '', label: ''});
        edges.push({from: source, to: '', label: '', rawText:edge.from});
        var sourceNode = { rawText: source, edges: edges, adjlist: adjlist, discriminative: true, labelled:true, alternative:false, initiallyFrequent:false, isPattern: true, debug_added_from: 'aaaa', debug_request_number: subgraph.request_number}

        nodes.push(sourceNode);

        // console.log('infer patterns:: one subgraph edge source = ' + source);
      }

      if (target) {
        target = target.replace(/\./g, '__');
        adjlist[target] = [];
        adjlist[target].push({to: '', label: ''});
        edges.push({from: target, to: '', label: '', rawText:edge.to});
        var targetNode = { rawText: target, edges: edges, adjlist: adjlist, discriminative: true, labelled:true, alternative:false, initiallyFrequent:false, isPattern: true, debug_added_from: 'aaaa', debug_request_number: subgraph.request_number}


        nodes.push(targetNode);

        // console.log('infer patterns:: one subgraph edge target = ' + target);
      }
    });

    // nodes.forEach(function (node) {
    //   Subgraphs.insert(node);
    // }); 
    // insert muiltiple nodes
    Subgraphs.batchInsert(nodes, function (error, result) {
      if (error) {
        console.log('[infer patterns] error when inserting new subgraph---->' + error);
      } else {
        // console.log('[infer patterns] inserted new subgraph with id ' + result);
      }
    });


    // hide the query examples, if there are any
    Examples.update({query: true}, {$set: {query: 'false'}}, {multi : true});
    

    // console.log('infer patterns!');
  },
  'addHint'({text, value}) {

    var adjlist = {};
    var edges = [];
    if (text.includes('->')) { // an edge was passed
      nodes = text.split('->');
      nodes = nodes.map(function (x) { 
        return x.trim().split(' ')[0]; 
      });
      var edgeType = text.split('->')[1].trim().split(' ')[1];
      adjlist[nodes[0]] = []
      adjlist[nodes[0]].push({to: nodes[1], label: edgeType});

      edges.push({from: nodes[0], to: nodes[1], label: edgeType, rawText:text});
      
    } else {
      adjlist[text] = []
      adjlist[text].push({to: '', label: ''});

      
      edges.push({from: text, to: '', label: '', rawText:text});
    }

    // delete the old hint if it exists
    Subgraphs.remove({rawText:text, hint: true});

    var newSubgraph = {
      rawText: text,
      hint: value,
      adjlist: adjlist,
      edges: edges
    };
    console.log('add hint')
    
    Subgraphs.insert(newSubgraph, function (error, result) {
      if (error) {
        console.log('[ addHint] error when inserting new subgraph---->' + error);
      } else {
        // console.log('[addHint] inserted new subgraph with id ' + result);
      }
    });


  },
  'createNewSubgraph'({text, checked, isPattern}) {

    var adjlist = {};
    var edges = [];

    if (text.includes(',')) { 
      nodes = text.split(',');

      console.log('creating new subgraph with ' + ' text=' + text + ' checked=' + checked + ' isPattern=' + isPattern);
      
      for (var i = 1; i < nodes.length; i++) {
        var node = nodes[i];

        var prevNode = nodes[i-1];

        var nodeWithoutEdge = node.includes('{') ? node.split('{')[0] : node;
        var prevNodeWithoutEdge = prevNode.includes('{') ? prevNode.split('{')[0] : prevNode;


        var label = prevNode.includes('{') ? prevNode.split('{')[1].split('}')[0] : '';
        console.log('label is ' + label + ' of ' + prevNode);
        
        adjlist[prevNodeWithoutEdge] = []
        adjlist[prevNodeWithoutEdge].push({to: nodeWithoutEdge, label: label});
        adjlist[nodeWithoutEdge] = []
        adjlist[nodeWithoutEdge].push({to: '', label: ''});

        edges.push({from: prevNodeWithoutEdge, to: nodeWithoutEdge, label: label, rawText:text});
      }
    } else if (text.includes('->')) { // an edge was passed
      nodes = text.split('->');
      nodes = nodes.map(function (x) { 
        return x.trim().split(' ')[0]; 
      });
      var edgeType = text.split('->')[1].trim().split(' ')[1];
      adjlist[nodes[0]] = []
      adjlist[nodes[0]].push({to: nodes[1], label: edgeType});

      edges.push({from: nodes[0], to: nodes[1], label: edgeType, rawText:text});
      
    } else {
      adjlist[text] = []
      adjlist[text].push({to: '', label: ''});

      
      edges.push({from: text, to: '', label: '', rawText:text});
    }

    var newSubgraph = {
      rawText: text,
      labelled: checked,
      discriminative: true,
      alternative: false,
      isPattern: isPattern,
      adjlist: adjlist,
      edges: edges
    };
    console.log('inserting newSubgraph', newSubgraph);
    Subgraphs.insert(newSubgraph, function (error, result) {
      console.log('error when inserting new subgraph---->' + error);
    });

     
      
    
  },
  'updateSubgraphs' ({subgraphId, text, checked, isPattern}) {
    console.log('updating subgraphs with _id=' + subgraphId + ' text=' + text + ' checked=' + checked + ' isPattern=' + isPattern);
    if (isPattern == null) {
      if (Subgraphs.find({rawText: text, discriminative: true}).count() <= 1 ) {
       var a  = Subgraphs.update( {_id: subgraphId} , {$set: {labelled: checked, discriminative: true, alternative: false, hint: null}});
      //  console.log('a = ' + a);
      }
    } else {
      if (Subgraphs.find({rawText: text, discriminative: true}).count() <= 1) {
       var a = Subgraphs.update( {_id: subgraphId} , {$set: {labelled: checked, discriminative: true, alternative: false, isPattern: isPattern, hint: null}});
      //  console.log('a = ' + a);
      }
    }
  },
  'updateNodeFeedback' ({subgraphIds}) {

    // fetch the subgraphs
    var subgraphs = Subgraphs.find({_id: {$in: subgraphIds}}).fetch();
    // console.log('[updateNodeFeedback] subgraphs are ' + JSON.stringify(subgraphs));

    // extract node names
    var nodeNames = [];
    subgraphs.forEach(function (subgraph) {
      var fromNodes = subgraph.edges.map(function (edge) {
        return edge.from;
      });
      var toNodes = subgraph.edges.map(function (edge) {
        return edge.to;
      }
      );
      nodeNames = nodeNames.concat(fromNodes).concat(toNodes);
      // filter away things like '<catch>'
      nodeNames = nodeNames.filter(function (x) {
        return !x.startsWith('<');
      });
    });

    var oldNodeNames = History.find({}).fetch().map(function (history) {
      return history.node;
    }) || [];

    nodeNames = nodeNames.concat(oldNodeNames);
    History.remove({});
    nodeNames.forEach(function (nodeName) {
      History.insert({node: nodeName});
    });
    // console.log('[updateNodeFeedback] nodeNames are ' + JSON.stringify(nodeNames));

    var nodesToInclude = nodeNames.filter(function (x, i, a) {
      return a.indexOf(x) == i;
    });

    var targetGraphId = -1;
    // given the exampleId and methodName, match the graph id based on java.security.MessageDigest__digest_graph_id_mapping.txt
    // the `java.security.MessageDigest__digest` files came from manually running scripts in the src2egroum2aug (HJGraphBuilderForActiveLearningInterface) project in eclipse
    
    var elementIdToGraphId = {};
    // exclude the examples that do not match the current view
    var selector = filterByViewAndKeyword(buildSkeleton(), 'all', '');
    Examples.find(selector).forEach(function (example) {
      elementIdToGraphId[example.exampleID] = example.graphId;
    });

    targetGraphId = -1;
    
    shellOutToMineSubgraphs(targetGraphId, {}, elementIdToGraphId,  "---dummy---", nodesToInclude, true, true);
  },

  'removeDiscriminative' ({subgraphId, text}) {
    console.log('[removeDiscriminative updating subgraphs with _id=' + subgraphId + ' text=' + text );

    // if (Subgraphs.find({rawText:text.replace(',-',''), discriminative:false}).count() == 0 && Subgraphs.find({rawText:text.replace(',+',''), discriminative:false}).count() == 0 && Subgraphs.find({rawText:text, discriminative:false}).count() == 0
    //        && Subgraphs.find({rawText: text, discriminative: false}).count() == 0 && Subgraphs.find({rawText:text +',-', discriminative:false}).count() == 0 && Subgraphs.find({rawText:text +',+', discriminative:false}).count() == 0) {
    //     Subgraphs.update( {_id: subgraphId} , {$set: {labelled: false, discriminative: false}});
    //     console.log('update discriminative subgraph with _id=' + subgraphId + ' text=' + text);
    // } else {
    var subgraph = Subgraphs.find({_id: subgraphId}).fetch()[0];
    // if (subgraph.alternative) {
    //   Subgraphs.update( {_id: subgraphId} , {$set: {labelled: false}});
    //   console.log('update discriminative subgraph with _id=' + subgraphId + ' text=' + text);
    // } else {
      Subgraphs.remove({_id: subgraphId});
      console.log('removed subgraph with _id=' + subgraphId + ' text=' + text);
    // }
    // }
  },
  'addSubgraphToBag' ({subgraphId, bag, color, image}) {
    console.log('[addSubgraphToBag] updating subgraphs with _id=' + subgraphId + ' bag=' + bag + ' color=' + color);
    // append to the bag field, which is an array
    Subgraphs.update(
      { _id: subgraphId }, // Query to find the subgraph document
      { $push: { bags: bag }, $set: {discriminative: false} } // Update operation to append the bag to the bags attribute
    );

    if (bag != null) {
      if (Bags.find({bagId : bag}).count() == 0) {
        console.log('no bag found with bagId=' + bag + ', inserting a new bag')
        Bags.insert({bagId : bag, color: color, image: image} );
      } else {
        // console.log('updating bag with bagId=' + bag + ' with labelled=' + checked);
        // Bags.update({bagId : bag}, {$set: {labelled: checked}});
      }
    }

  },
  'editBag' ({ bag}) {
    console.log('[editBag] updating subgraphs with  bag=' + bag);
    
    // move current discriminative subgraphs back to frequent subgraphs
    Subgraphs.find({discriminative: true}).forEach(function (subgraph) {
      var text = subgraph.rawText;

      if (Subgraphs.find({rawText:text.replace(',-',''), discriminative:false}).count() == 0 && Subgraphs.find({rawText:text.replace(',+',''), discriminative:false}).count() == 0 && Subgraphs.find({rawText:text, discriminative:false}).count() == 0 ) {
        if (Subgraphs.find({rawText: text, discriminative: false}).count() == 0 && Subgraphs.find({rawText:text +',-', discriminative:false}).count() == 0 && Subgraphs.find({rawText:text +',+', discriminative:false}).count() == 0) {
          Subgraphs.update(
            { _id: subgraph._id }, // Query to find the subgraph document
            { $set: {discriminative: false} } // Update operation to append the bag to the bags attribute
          );
        }
      }
    });
    // also remove the subgraphs on 'preview' 
    Subgraphs.find({preview: true}).forEach(function (subgraph) {
      Subgraphs.update( {_id: subgraph._id} , {$set: {labelled: true, discriminative: false, preview: false}});

    });
      
    


    // change all subgraphs with bag to be discriminative:
    // and remove the bag from the bags attribute
    var a = Subgraphs.update(
      { bags: parseInt(bag) }, // Query to find the subgraph document
      { $set: {discriminative: true}, $pull: { bags: parseInt(bag) } } // Update operation to remove the bag to the bags attribute
    );
    console.log('pulled bag ' + bag + ' from all subgraphs')
    console.log(a);

   

  },

  'computeQueryExamples'() {
    // reset examples with query

    var hasQuerySelector = {'$and': [{query: {$exists: true}}]};
    // var selector = constructSelectorToFilterBaggedPatterns(hasQuerySelector);
    Examples.find(hasQuerySelector).forEach(function (example) {

      Examples.update({_id: example._id}, {$set: {query: null}});
    });
    console.log('computeQueryExamples, reset done');

    shellOutToComputeQueryExamples();



  },
  'clearHints'() {
    console.log('clearHints');
    Subgraphs.update({hint: true}, {$set: {hint: false}}, {multi: true});
  },
  'endTask' ({subjectNum, isBaseline}) {

    console.log('end task!');
    console.log(subjectNum);

    // record the discrimatinve subgraphs to file
    var subgraphs = Subgraphs.find({ '$and' : 
      [
        {'discriminative': true},  
        {'hidden': {'$ne': true}},
        {'$or': [{ 'bags': { '$exists': false } }, { 'bags': { $eq: null } }]}
      ]
    }).fetch();
    var discriminativeSubgraphsText =  subgraphs.map(function (subgraph) {
      return subgraph.rawText;
    }
    ).join('\n');
    console.log(discriminativeSubgraphsText);

    // write to file
    var fs = require('fs');
    var path = require('path');
    var dir = path.resolve('.').split('.meteor')[0];
    var baseline = isBaseline ? '_baseline' : '';
    var filename = dir + '_' + subjectNum + '_' + baseline + '_discriminativeSubgraphs.txt';
    
    var collectData = false;

    if (collectData) {
      console.log(filename);
      fs.writeFile(filename, discriminativeSubgraphsText, function(err) {
        if(err) {
          return console.log(err);
        }
        console.log("The file was saved!");
      }
      );
    }

  
  },

  'getOpenaiCompletion' ({API}) {
    // const completions =  getOpenaiCompletion(API);
    // console.log(completions);

    var completions = getSmplMixup(API);
    console.log(completions);
  },

  'resetState' () {
    resetDatabase();

    Config.insert({APIshortName: APIshortName, API: API});

  }

});
