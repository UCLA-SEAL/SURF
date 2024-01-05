import { Template } from 'meteor/templating';
import { ReactiveDict } from 'meteor/reactive-dict';
import { Session } from 'meteor/session';

import { Examples, TestExamples } from '../api/options.js';
import { ActionLog } from '../api/actionlog.js';
import { Subgraphs } from '../api/subgraphs.js';
import { Bags } from '../api/bags.js';

import { Queries } from '../api/queries.js';

import { Config } from '../api/config.js';

import Pycollections from 'pycollections';

import html2canvas from 'html2canvas';


import 'bootstrap/dist/css/bootstrap.min.css'
import hljs from 'highlight.js'
import 'highlight.js/styles/github-dark.css'

import './body.html'; 

window.Subgraphs  = Subgraphs;
window.Examples = Examples;
window.Config = Config;
window.TestExamples = TestExamples;

var getAllSubgraphs = function(isDiscriminative) {
  var uniqueCombinationOfNodes = {};
    var result = [];
    Subgraphs.find({}).forEach(function(subgraph) {
      var nodes = {};
      subgraph.edges.forEach(function(edge) {
        nodes[edge.from] = true;
        if (edge.to != '') {
          nodes[edge.to] = true;
        }
        
      });
      var nodeStr = Object.keys(nodes).sort().join('-');
      if (subgraph.discriminative != isDiscriminative) {
        return;
      }
      if (!uniqueCombinationOfNodes[nodeStr] && nodeStr != '') {
        uniqueCombinationOfNodes[nodeStr] = true;

        result.push(subgraph.subgraphId);
      }
    });
    return result;
}

var exampleTotal = function(){
  // console.log('exampleTotal',Examples.find({dataset: getDataset()}).count());
  return fetchShortestExamples({dataset: getDataset()}).count();
  // return fetchExamples(selector).count();
}


var optimalLabellingTotal = function() {
  return fetchShortestExamples({dataset: getDataset(), label: { '$in' : ['positive', 'negative']}}).count();
}

var getFocalNode = function() {
  var config = Config.findOne({});
  var API = config.API;
  var APIfocalNode;
  if (API.includes('javax.crypto.Cipher__init')) {

      APIfocalNode = "Cipher.getInstance()";
  } else if (API.includes('java.security.MessageDigest__digest')) {
      APIfocalNode = "MessageDigest.getInstance()";
    } else if (API.includes('java.security.SecureRandom__Key')) {
      APIfocalNode = "SecureRandom.<init>";
  } 
  return APIfocalNode;
}

var getFocalAPI = function() {
  var config = Config.findOne({});
  return config.API.split('.')[2] + '()';
}

var getDataset = function() {
  var config = Config.findOne({});
  return config.APIshortName;
}

var getAPI = function() {
  var config = Config.findOne({});
  return config.API;
}





var fetchExamples = function(selector){
  if (_.isEmpty(selector)){
    selector = {'dataset': getDataset()};
  } else {
    selector['dataset'] = getDataset();
  }
  return fetchShortestExamples(selector).fetch();
}

var fetchLabelledExamples = function(selector, label){
  if (_.isEmpty(selector)){
    selector = {'dataset': getDataset()};
  } else {
    selector['dataset'] = getDataset();
  }
  selector['label'] = { '$in' : [label]};
  return fetchShortestExamples(selector).fetch();
}

var fetchAndCountExamples = function(selector){
  if (_.isEmpty(selector)){
    selector = {'dataset': getDataset()};
  } else {
    selector['dataset'] = getDataset();
  }
  // filter out examples without a corresponding graph
  // if (!selector['$and']){
  //   selector['$and'] = [];
  // }
  // selector['$and'].push( {'graphId' : { '$exists' : true } } );
  return Examples.find(selector).count();
}

var fetchAndCountLabelledExamples = function(selector, label) {
  var isEmptySelector = false;
  if (_.isEmpty(selector)){
    selector = {'dataset': getDataset()};
    isEmptySelector = true;
  } else {
    selector['dataset'] = getDataset();
  }
  if (!selector['$and']){
    selector['label'] = label;
  } else {
    selector['$and'].push( {'label' : label});
  }
  return {
    'match' : isEmptySelector ? 0 : Examples.find(selector).count(),
    'missed' : Examples.find({'dataset': getDataset(), 'label' : label }).count()
  }
}


var computeSelectorByViewAndKeyword = function(skeleton){
  // account for the view type
  var viewType = Session.get('view');
  // if (viewType === 'all' && _.isEmpty(skeleton)){ // we're in a weird state, just show matching examples then
  //   viewType = 'matching';
  //   Session.set('view', 'matching');
  // }
  if (viewType === 'all' || !viewType) {
    // default behavior
    var selector = {} // constructSelectorToFilterBaggedPatterns({});
    
  } else if (viewType === 'matching') {
    if (_.isEmpty(skeleton)){
      return {'dummy': false}; // do not match anything
    }
    // apply the skeleton selector
    var selector = computeSelectorFromSkeleton(skeleton);
    // selector = constructSelectorToFilterBaggedPatterns(selector);
    

  } else if (viewType === 'unlabelled') {
    // select examples not labelled
    var selector =  { '$and' : [{'label': {'$nin': [ 'positive', 'negative']}}] };
    // selector = constructSelectorToFilterBaggedPatterns(selector);

    
  } else if (viewType === 'labelled') {
    // select examples not labelled
    var selector =  { '$and' : [{'label': {'$in': [ 'positive', 'negative']}}] };
    // selector = constructSelectorToFilterBaggedPatterns(selector);

  } else if (viewType === 'confused') {
    if (_.isEmpty(skeleton)){
      return {'dummy': false}; // do not match
    }
    
    // select examples labelled, but mismatching the skeleton
    // 1. select examples matching the skeleton, but are negative
    // 2. select examples not matching the skeleton, but are positive
    var selector = computeSelectorFromSkeleton(skeleton);
    selector['$and'] = [{'label': 'negative'}];

    // selector = constructSelectorToFilterBaggedPatterns(selector);

    // var first = fetchShortestExamples(selector);
    // return first.fetch();
    // var secondSelector = {'$and': [{'label': 'positive'}, {'$nor': computeSelectorFromSkeleton(skeleton)['$and']}]};
    // secondSelector = constructSelectorToFilterBaggedPatterns(secondSelector);
    // var second = fetchShortestExamples(secondSelector);
    
    // return first.fetch().concat(second.fetch());
  } else if ( viewType === 'not-matching') {
    if (_.isEmpty(skeleton)){
      return {'dummy': false}; // do not match
    }
    // select examples not matching the skeleton
    var selector = {'$and': [{'label': 'positive'}, {'$nor': computeSelectorFromSkeleton(skeleton)['$and']}]};
    // selector = constructSelectorToFilterBaggedPatterns(selector);

  }
  // keyword search
  var keyword  = Session.get('keyword');
  if (keyword) {  
    const regex = new RegExp(keyword);
    const query = { codeElements: regex };

    if (!selector['$and']){
      selector['$and'] = [];
    }
    selector['$and'].push(query);
  }

  return selector;
}



var filterByView = function(skeleton){
  return fetchShortestExamples(constructSelectorToFilterBaggedPatterns(computeSelectorByViewAndKeyword(skeleton)));
}

var fetchShortestExamples = function(selector){
  if (_.isEmpty(selector)){
    selector = {'dataset': getDataset()};
  } else {
    selector['dataset'] = getDataset();
  }
  // filter out examples without a corresponding graph
  if (!selector['$and']){
    selector['$and'] = [];
  }
  selector['$and'].push( {'graphId' : { '$exists' : true } } );

  // hack over edges.. ignore directions for non-order
  selector['$and'] = selector['$and']
  .map(function(item){
    var newItem = {};
    Object.keys(item).forEach(function(key){
      if (key.includes('->') 
      // && !key.includes("order")
      ) {
        var node1 = key.split(' -> ')[0];
        var node2 = key.split(' -> ')[1].split(' ')[0];
        var newEdge = node2 + ' -> ' + node1 + ' ' + key.split(' -> ')[1].split(' ')[1];
        var newObj = {};
        newObj[newEdge] = item[key];
        var newKey = '$or';
        newItem[newKey] = [item, newObj];
      } else {
        var newKey = key;
        newItem[newKey] = item[key];
      }
      
    });
    return newItem;  
  });

  // console.log('selector',selector);
  return Examples.find(selector, { sort: { codeLength : 1 } });
}

var fetchNShortestExamples = function(selector, n){
  if (_.isEmpty(selector)){
    selector = {'dataset': getDataset()};
  } else {
    selector['dataset'] = getDataset();
  }
  // filter out examples without a corresponding graph
  if (!selector['$and']){
    selector['$and'] = [];
  }
  selector['$and'].push( {'graphId' : { '$exists' : true } } );
  // console.log('selector',selector);
  return Examples.find(selector, { sort: { codeLength : 1 }, limit: n });
}

var misusesMatched = function(bagId) {
  // first pick all bags chronologically earlier than this one
  var oldBags = Bags.find({ bagId: { $lt: bagId } }).fetch();

  // first match examples that match all the old bags
  var selector = {};
  var allMatchedExamples = [];
  _.each(oldBags, function(bag) {
    // pick subgraphs from this bag
    var subgraphs = Subgraphs.find({ bags: bag.bagId }).fetch();
    if (subgraphs.length == 0) return;

    var selector = computeSelector(subgraphs);
    var matchedExamples = fetchLabelledExamples(selector);
    allMatchedExamples.push(...matchedExamples);
  });

  var subgraphs = Subgraphs.find({ bags: bagId })
    .fetch();
  var selector = computeSelector(subgraphs);
    
  // selector['bags'] = bagId;

  var matchedExamples = fetchLabelledExamples(selector, 'negative');
  var newlyMatchedExamples = _.difference(matchedExamples, allMatchedExamples);
  return newlyMatchedExamples.length;
}

var createNewExample = function(text, label) {
  Meteor.call('createNewExample', {
    text: text,
    label: label,
    dataset: getDataset(),
    view: Session.get('view'),
    keyword: Session.get('keyword')
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[createNewExample] succeded in creating new example')

    }
  });

}

var connectToRepo = function(path) {
  Meteor.call('connectToRepo', {
    path: path,
    dataset: getDataset(),
    view: Session.get('view'),
    keyword: Session.get('keyword')
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[connectToRepo] succeded in creating new example')

    }
  });

}

var updateLabels = function(exampleId, methodName, label, keyword, nextFunction) {
  // console.log(instance);

  Meteor.call('updateLabels', {
    exampleId: exampleId,
    methodName: methodName,
    labels: label,
    view: Session.get('view'),
    keyword: keyword,
    focalNode: getFocalNode(),
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      // console.log('[updateLabels] succeded in updating labels')
      clearSummary();
      if (nextFunction)
        nextFunction();
    }
  });
}

var updateNodeFeedback = function(hintSubgraphs, nextFunction) {
  Meteor.call('updateNodeFeedback', {
    subgraphIds: hintSubgraphs.map(function(subgraph){
      return subgraph._id
    }),
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[updateNodeFeedback] succeded')
      
      if (nextFunction)
        nextFunction();
    }
  });
}

var endTask = function(nextFunction) {
  // console.log(instance);

  Meteor.call('endTask', {
    subjectNum: Session.get('subjectNum'),
    isBaseline: Session.get('isBaseline'),
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[endTask] succeded')
      
      if (nextFunction)
        nextFunction();
    }
  });
}

var deleteDiscriminativeSubgraphs = function() {

  Meteor.call('deleteDiscriminativeSubgraphs', {
    view: Session.get('view'),
    keyword: Session.get('keyword'),
    focalNode: getFocalNode(),
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[deleteDiscriminativeSubgraphs] succeded in deleting discrimiantive subgraphs')
      clearSummary();
    }
  });
}

var resetLabels = function() {

  Meteor.call('resetLabels', {
    view: Session.get('view'),
    keyword: Session.get('keyword'),
    focalNode: getFocalNode(),
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[resetLabels] succeded in reset labels')
      clearSummary();
    }
  });
}

var clearHints = function() {
  Meteor.call('clearHints', {
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[clearHints] succeded in clearHints')
      // clearSummary();
    }
  });

}

var resetState = function() {
  Meteor.call('resetState', {
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[resetState] succeded in resetState')
      // clearSummary();
    }
  });
}

var computeQueryExamples = function(){

  Meteor.call('computeQueryExamples', 
    {}, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[computeQueryExamples] succeded in updating labels')
    }
  });

}

var inferPatterns = function() {

  Meteor.call('inferPatterns', {
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[inferPatterns] succeded in updating labels')
      // clearSummary();
    }
  });
}



var createNewSubgraph = function(text, checked, isPattern, nextFunction) {

  Meteor.call('createNewSubgraph', {
    text: text,
    checked: checked,
    isPattern: isPattern,
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[createNewSubgraph] succeded in creating new subgraph')

      nextFunction();
    }
  });
}

var addHint = function(text, value, nextFunction) {
  Meteor.call('addHint', {
    text: text,
    value: value
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[addHint] succeded in creating new subgraph')

      nextFunction();
    }
  });
}

var updateSubgraphs = function(subgraphId, text, checked, isPattern, bag, nextFunction) {
  console.log('updateSubgraphs', subgraphId);

  Meteor.call('updateSubgraphs', {
    subgraphId: subgraphId,
    text: text,
    checked: checked,
    isPattern: isPattern,
    bag: bag,

  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('[updateSubgraphs] succeded in updating subgraphs')

      nextFunction();
    }
  });
}


var addSubgraphToBag = function(subgraphId, bag, color, image, nextFunction) {
  console.log('addSubgraphToBag', subgraphId, bag, color);

  Meteor.call('addSubgraphToBag', {
    subgraphId: subgraphId,
    bag: bag,
    color: color,
    image: image
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('succeded in updating subgraphs')

      nextFunction();
    }
  });
}

var removeDiscriminative = function(subgraphId, text, nextFunction) {
  console.log('removeDiscriminative', subgraphId, text);

  Meteor.call('removeDiscriminative', {
    subgraphId: subgraphId,
    text: text
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('succeded in updating subgraphs')

      nextFunction();
    }
  });
}

var editBag = function(bagId,  nextFunction) {
  console.log('editBag', bagId);

  Meteor.call('editBag', {
    bag: bagId,

  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('succeded in editBag')

      nextFunction();
    }
  });
}

var getOpenaiCompletion = function(nextFunction) {

  Meteor.call('getOpenaiCompletion', {
    API: getFocalAPI(),
  }, (err, res) => {
    if (err) {
      alert(err);
    } else {
      // success!
      console.log('succeded in getOpenaiCompletion')

      nextFunction(res);
    }
  });
}

var filterNodeLabel = function(nodeLabel) {
  return nodeLabel != '' && !nodeLabel.includes("pseudo");;
  
  // return nodeLabel.includes('<') ||  nodeLabel.includes('(') || nodeLabel.includes(':') || nodeLabel.includes("__");     
}

var filterOnlyLiteralsAndCalls = function(nodeLabel) {
  // if (nodeLabel.includes('this__') && !nodeLabel.includes('(')) {
  //   return false;
  // }
  return  nodeLabel.includes('(') || nodeLabel.includes(':') || nodeLabel.includes("__");
   // nodeLabel.includes('<') || 
}

var filterOnlyLiteralsAndExceptionsAndCalls = function(nodeLabel) {
  // if (nodeLabel.includes('this__') && !nodeLabel.includes('(')) {
  //   return false;
  // }
  return nodeLabel.includes("Exception") || nodeLabel.includes('(') || nodeLabel.includes(':') || nodeLabel.includes("__");
   // nodeLabel.includes('<') || 
}

var allNodes = function() {
  var skeleton = Session.get('skeleton');
  var allExamples = filterByView(skeleton).fetch();
  var allNodes = _.uniq(allExamples.flatMap(function(example) {
    return example.codeElements.filter(element => !element.includes("->")).map(element => element.replaceAll('.', '__'));
  }
  ));
  return allNodes;
}

var buildSkeleton = function(){
  
  // if (Session.get('skeleton') && Object.keys(Session.get('skeleton')).length > 0) {
  //   return;
  // }

  var nodeContainedInSubgraph = {};
  
  var nodeContainedInCorrectSubgraph = {};
  var nodeContainedInMisuseSubgraph = {};

  var mustMatchEdges = {};

  var nodes = {};
  Subgraphs.find( { '$or' : [{'$and': [{discriminative: true, hidden: {'$ne' : true }}]} ] }).forEach(function(subgraph) {
    
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

      if (edge.from && edge.to) {
        var edgeKey = edge.from + ' -> ' + edge.to + ' ' + edge.label + '';
        // if (!mustMatchEdges[edgeKey]) {
          mustMatchEdges[edgeKey] = true;
        // }
        
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

  // pairwise constraints
  Object.keys(mustMatchEdges).forEach(function(edgeKey) {
    skeleton[edgeKey] = {
      'text': edgeKey,
      'checked': true,
      'matchStatus': 'correct',
    }
  });

// 
  // console.log('running buildSkeleton completed!' )
  // console.log(skeleton);

  // if (Object.keys(skeleton).length > 0) {
    // console.log('running buildSkeleton completed!' + skeleton)
  Session.set('skeleton', skeleton);
  // }

}

var identifyElementRoles = function() {
  // fetch examples
  var examples = fetchShortestExamples();

  var elementToOutgoingType = {};
  var elementToIncomingType = {};
  examples.forEach(function(example) {
    // get the code elements
    var edges = Object.keys(example).filter(function(element) {
      return element.indexOf(' -> ') > -1;
    });
    edges.forEach(function(edge) {

      var from = edge.split(' -> ')[0];
      var to = edge.split(' -> ')[1].split(' ')[0];
      var edgeLabel = edge.split(' -> ')[1].split(' ')[1];
  
      if (!elementToOutgoingType[from]) {
        elementToOutgoingType[from] = {};
      }
      elementToOutgoingType[from][edgeLabel] = (elementToOutgoingType[from][edgeLabel] || []);
      elementToOutgoingType[from][edgeLabel].push(to);

      if (!elementToIncomingType[to]) {
        elementToIncomingType[to] = {};
      }
      elementToIncomingType[to][edgeLabel] = (elementToIncomingType[to][edgeLabel] || []);
      elementToIncomingType[to][edgeLabel].push(from);
    });
  });

  var elementsToRole = {};

  
  Object.keys(elementToOutgoingType).forEach(function(element) {
    // a guard is an element that has outgoing "sel"
    if (elementToOutgoingType[element] && elementToOutgoingType[element]['(sel)'] && elementToOutgoingType[element]['(sel)'].indexOf("<throw>") == -1) {
      elementsToRole[element] = (elementsToRole[element] || []).concat('guard');
    }

    // a pre-method call is a method with an order to our focal API
    if (elementToOutgoingType[element] && !element.includes("<init>") && elementToOutgoingType[element]['(order)'] && elementToOutgoingType[element]['(order)'].indexOf(getFocalNode().replace('.','__')) > -1) {
      
      elementsToRole[element] = (elementsToRole[element] || []).concat('method');
    }
    // a post-method call is a method with an order from our focal API
    else if (elementToIncomingType[element] && !element.includes("<init>") && elementToIncomingType[element]['(order)'] && elementToIncomingType[element]['(order)'].indexOf(getFocalNode().replace('.','__')) > -1) {
      
      elementsToRole[element] = (elementsToRole[element] || []).concat('method');
    }

    // a parameter is an element that has an outgoing "param"
    if (elementToOutgoingType[element] && elementToOutgoingType[element]['(para)'] && !element.includes("Exception") && !element.includes("Error")) {
      elementsToRole[element] = (elementsToRole[element] || []).concat('parameter1');
    }

    // an exception is an element that is parameter to "<catch>"
    if (elementToIncomingType[element] && elementToIncomingType[element]['(para)'] && elementToIncomingType[element]['(para)'].indexOf("<catch>") > -1) {
      elementsToRole[element] = (elementsToRole[element] || []).concat('exception');
    }
    if (elementToOutgoingType[element] && elementToOutgoingType[element]['(para)'] && elementToOutgoingType[element]['(para)'].indexOf("<catch>") > -1) {
      elementsToRole[element] = (elementsToRole[element] || []).concat('exception');
    }

  

    // a return value is an element that has an incoming "return"
    if (elementToIncomingType[element] && elementToIncomingType[element]['(return)']) {
      elementsToRole[element] = (elementsToRole[element] || []).concat('return');
    }

    // a declaration is an element that has an incoming "decl"
    // if (elementToIncomingType[element] && elementToIncomingType[element]['(def)']) {
      if (element.includes("<init>") && !element.includes("Exception") && !element.includes("Error") ) {
        elementsToRole[element] = (elementsToRole[element] || []).concat('declaration');
      }
    // }

    // error handling is an element that has an incoming "catch"
    if (elementToIncomingType[element] && elementToIncomingType[element]['(hdl)']) {
      elementsToRole[element] = (elementsToRole[element] || []).concat('error');
    }


  });

  return  elementsToRole;
}


var clearSummary = function() {
  Session.set('summary', {});
}

var buildSummary = function() {
  if (Session.get('summary') && Object.keys(Session.get('summary')).length > 0) {
    return;
  }
  
  var viewType = Session.get('viewType');
  var subgraphs = subgraphsToDisplay(viewType);
  var sortedNodes = sortNodes(subgraphs);


  var summary = {};  
  var addToSummary = function(node) {
    summary[node.text] = {
      'text': node.text,
      'color': node.contextColor
    }
    if (node.children) {

      // sort the children alphabetically
      node.children = node.children.sort(function(a, b) {
        if (a.text < b.text) {
          return -1;
        } else if (a.text > b.text) {
          return 1;
        } else {
          return 0;
        }
      });

      // for loop over the children
      for (var i = 0; i < node.children.length; i++) {
        var child = node.children[i];
        addToSummary(child);
      }
    }
  };
  sortedNodes.forEach(addToSummary);
  
// 
  
  Session.set('summary', summary);
  console.log(Object.keys(summary).length);
}

var blocknames = [
  'initialization',
  'try',
  'configuration',
  'guardType',
  'guardCondition',
  'focalAPI',
  'checkType',
  'followUpCheck',
  'use',
  'exceptionType',
  'exceptionHandlingCall',
  // 'finally',
  'cleanUpCall'
];
// var blocknames = [];

var option_lists = [
  'initialization',
  'exceptionHandlingCall',
  'configuration',
  'use',
  'cleanUpCall'
];

console.log('blocknames');
console.log(blocknames)



var addSpan = function(exampleID, clusterContext, expressionStart,expressionEnd,blockname){
  if (expressionStart !== -1 && expressionEnd !== -1) {
    try {
      var clusterIdentifier = clusterContext ? '-' + clusterContext : '';
    spanAdder.addRegionsD3('#exampleID'+exampleID + clusterIdentifier,expressionStart,expressionEnd,blockname);
    } catch (e) {
      console.log('err -> ' + e);
    }
  } 
}
var removeSpan = function(exampleID,expressionStart,expressionEnd,blockname){
  if (expressionStart !== -1 && expressionEnd !== -1) {
    try {
      $('#exampleID' + exampleID + ' .' + blockname).removeClass(blockname);
      // console.log('remove span from ' + '#exampleID' + exampleID + ' .' + blockname);
    } catch (e) {
      console.log('err -> ' + e);
    }
  } 
}




const escapeHtml = (unsafe) => {
  return unsafe.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#039;');
}

var getTextDescendants = function (node) {
  var descendants = [];
  var i, j;
  var child;
  var childDescendants;
  if (node.nodeName === '#text') {
      descendants.push(node);
  } else {
      for (i = 0; i < node.childNodes.length; i++) {
          child = node.childNodes[i];
          childDescendants = getTextDescendants(child);
          for (j = 0; j < childDescendants.length; j++) {
              descendants.push(childDescendants[j]);
          }
      }
  }
  return descendants;
};

var getRangeInText = function (textNodeList, startOffset, endOffset) {

  var currentOffset = 0;
  var nodeStartOffset, nodeEndOffset;
  var startNode, endNode;
  var foundStart = false, foundEnd = false;

  textNodeList.some(function (node) {
      var textLength = node.textContent.length;
      if (startOffset >= currentOffset && 
          startOffset < currentOffset + textLength) {
          foundStart = true;
          startNode = node;
          nodeStartOffset = startOffset - currentOffset;
      }
      if (endOffset >= currentOffset &&
          endOffset <= currentOffset + textLength) {
          foundEnd = true;
          endNode = node;
          nodeEndOffset = endOffset - currentOffset;
      }
      currentOffset += textLength;
      return foundStart && foundEnd;
  });

  return {
      'start': {
          'node': startNode,
          'offset': nodeStartOffset
      },
      'end': {
          'node': endNode,
          'offset': nodeEndOffset
      }
  };

};

var render = function(obj) {
  // hljs.highlightAll();

  // console.log('length = ' + $("#exampleID12 pre").length);
  

  // first, clear all the spans
  // $('.example-highlighted-misuse').removeClass('example-highlighted-misuse');
  // $('.example-highlighted-correct').removeClass('example-highlighted-correct');
  // $('.example-highlighted-confused').removeClass('example-highlighted-confused');
  var clusterContext = obj['clusterContext'] ? '-' + obj['clusterContext'] : '';
  $('#exampleID' + obj['exampleID'] + clusterContext + ' pre').removeClass('example-highlighted-confused  example-highlighted-confused-correct example-highlighted-confused-misuse example-highlighted-correct example-highlighted-misuse');

  // match against `subgraph-0-node`
  $('#exampleID' + obj['exampleID'] + clusterContext + '  .subgraph-0-node').removeClass('subgraph-0-node');
  $('#exampleID' + obj['exampleID'] + clusterContext + '  .subgraph-0-node-correct').removeClass('subgraph-0-node-correct');
  $('#exampleID' + obj['exampleID'] + clusterContext + '  .subgraph-0-node-misuse').removeClass('subgraph-0-node-misuse');
  
  $('#exampleID' + obj['exampleID'] + clusterContext + ' .example-highlighted-correct').removeClass('example-highlighted-correct');
  $('#exampleID' + obj['exampleID'] + clusterContext + ' .example-highlighted-misuse').removeClass('example-highlighted-misuse');
  

  $('#exampleID' + obj['exampleID'] + clusterContext + '  .keyword-highlight').removeClass('keyword-highlight');
  $('#exampleID' + obj['exampleID'] + clusterContext + ' .boundedBox').removeClass('boundedBox');
  $('#exampleID'  + obj['exampleID'] + clusterContext + ' .match-explanation').addClass('hidden');
  $('#exampleID'  + obj['exampleID'] + clusterContext + ' span').css('background-color', '');

  var additionalNodeToHighlight = $('#exampleID'  + obj['exampleID'] + clusterContext).attr('data-additional');
   

  // color the spans using the keyword
  var keyword = Session.get('keyword');

  // first, clear existing 'keyword-highlight'
  var exampleID = obj['exampleID'];

  var focalAPI = getFocalAPI();
  
  // console.log(exampleID);
  // console.log(obj);
  // check if any of the obj keys match the keyword
  if (obj['codeElements'] == null) {
    console.log('odd!')
  }
  obj['codeElements'].forEach(function(key) {
    // console.log('key', key);
    if (keyword && keyword.length > 2 && key.indexOf(keyword) > -1) {

      key = key.replace(/\./g, '__');
      var expressionStart = obj[key]['expressionStart'];
      var expressionEnd = obj[key]['expressionEnd'];
      
      // var partialExpressionEnd = expressionStart + keyword.length;
      addSpan(exampleID, obj['clusterContext'], expressionStart, expressionEnd, 'keyword-highlight');
      
      // console.log('highlight keyword ' + exampleID + ' ' + expressionStart + ' ' + expressionEnd);

    } 
  });
  // hghlight the code with focalAPI
  if (focalAPI && focalAPI.length > 2 && Object.keys(obj).indexOf(focalAPI) > -1) {
    // var expressionStart = obj[focalAPI']['expressionStart'];
    // var expressionEnd = obj[focalAPI]['expressionEnd'];


    // addSpan(exampleID, obj['clusterContext'], obj[focalAPI]['expressionStart'], obj[focalAPI]['expressionEnd'], 'boundedBox');

    
    // console.log('highlight focalAPI ' + exampleID + ' ' + obj[focalAPI]['expressionStart'] + ' ' + obj[focalAPI]['expressionEnd']);
  }

  // highlight 'additional' nodes if they exist
  if (additionalNodeToHighlight) {

    var expressionStart = obj[additionalNodeToHighlight]['expressionStart'];
    var expressionEnd = obj[additionalNodeToHighlight]['expressionEnd'];

    addSpan(exampleID, obj['clusterContext'], expressionStart, expressionEnd, 'keyword-highlight');
  }

  // console.log('highlight code');
  


  buildSkeleton();
  // buildSummary();
  var skeleton = Session.get('skeleton');
  // var summary = Session.get('summary');
  if (_.isEmpty(skeleton)) {
    return;
  }

  // don't visualize the skeleton if the user is looking at the labelled view
  if (clusterContext == '-labelled') {
    skeleton = {};
  }

  var correctSkeletonKeys = Object.keys(skeleton).filter(function(key) {
    return skeleton[key]['checked'] && skeleton[key]['matchStatus'] === 'correct';
  }).map(function(key) {
    return key.replace(/\./g, '__');
  });

  var misuseSkeletonKeys = Object.keys(skeleton).filter(function(key) {
    return skeleton[key]['checked'] &&  skeleton[key]['matchStatus'] === 'misuse';
  }).map(function(key) {
    return key.replace(/\./g, '__');
  });

  var objKeys = Object.keys(obj);


  var correctSkeletonKeysSet = new Set(correctSkeletonKeys);
  var misuseSkeletonKeysSet = new Set(misuseSkeletonKeys);
  var objKeysSet = new Set(objKeys);
  
  var correctIntersection = new Set([...correctSkeletonKeysSet].filter(x => objKeysSet.has(x)));
  var misuseIntersection = new Set([...misuseSkeletonKeysSet].filter(x => objKeysSet.has(x)));

  // always reset the spans and colors
  for (let node of Object.keys(skeleton)) {
    var exampleID = obj['exampleID'];

    var nodeText = skeleton[node]['text'];

    if (!skeleton[node]['checked']) {
      if (!_.isEmpty(obj[nodeText])) {
        var expressionStart = obj[nodeText]['expressionStart'];
        var expressionEnd = obj[nodeText]['expressionEnd'];

        skeleton[node]['allSubgraphs'].forEach(function(subgraphId){
          removeSpan(exampleID, expressionStart, expressionEnd, 'subgraph-' + subgraphId + '-node');
          console.log('remove span (skeleton)' + exampleID + ' ' + expressionStart + ' ' + expressionEnd);
        });
      } 
    } 
  }

  var matchMisuse = misuseIntersection.size === misuseSkeletonKeysSet.size && misuseSkeletonKeysSet.size > 0;
  var matchCorrect = correctIntersection.size === correctSkeletonKeysSet.size && correctSkeletonKeysSet.size > 0;

  // color the example
  

  
  // if (matchMisuse || !matchCorrect) {
  //   $('#exampleID' + obj['exampleID'] + clusterContext  + ' pre').removeClass('example-highlighted-misuse example-highlighted-correct example-highlighted-confused opacity-50');
  //   $('#exampleID' + obj['exampleID'] + clusterContext  + ' pre').addClass('example-highlighted-misuse');
    
  //   if (obj.label == 'negative') {
  //     $('#exampleID' + obj['exampleID'] + clusterContext  + ' pre').addClass('opacity-50');
  //   } else if (obj.label == 'positive'){
  //     // highlight this example
  //     $('#exampleID' + obj['exampleID'] + clusterContext  + ' pre').addClass('example-highlighted-confused-misuse');
  //   }
  // } else if (matchCorrect && !matchMisuse) {
  //   $('#exampleID' + obj['exampleID'] + clusterContext  + ' pre').removeClass('example-highlighted-misuse example-highlighted-correct example-highlighted-confused opacity-50');
  //   $('#exampleID' + obj['exampleID'] + clusterContext  + ' pre').addClass('example-highlighted-correct');
    
  //   if (obj.label == 'positive') {
  //     $('#exampleID' + obj['exampleID'] + clusterContext  + ' pre').addClass('opacity-50');
  //   } else if (obj.label == 'negative') {
  //     // highlight this example
  //     $('#exampleID' + obj['exampleID'] + clusterContext  + ' pre').addClass('example-highlighted-confused-correct');
  //   }
  // }

  var explanation = '';
  var notMatch = [];
  if (!matchMisuse) {
      // join the elements of the set misuseIntersection
    explanation = Array.from(misuseSkeletonKeysSet).map(text => '<span class="subgraph-0-node-misuse">' + escapeHtml(text) + '</span>').join(',');
  }

  for (let node of Object.keys(skeleton)) {
    var exampleID = obj['exampleID'];
    // console.log('exampleID',exampleID);
    var nodeText = skeleton[node]['text'];

    if (!skeleton[node]['checked']) {
      if (!_.isEmpty(obj[nodeText])) {
        var expressionStart = obj[nodeText]['expressionStart'];
        var expressionEnd = obj[nodeText]['expressionEnd'];

        if (Object.keys(skeleton[node]).includes('allSubgraphs')) {
          skeleton[node]['allSubgraphs'].forEach(function(subgraphId){

            // console.log('length = ' + $("#exampleID12 pre").length);

            removeSpan(exampleID, expressionStart, expressionEnd, 'subgraph-' + subgraphId + '-node');
            removeSpan(exampleID, expressionStart, expressionEnd, 'subgraph-' + subgraphId + '-node-correct');
            removeSpan(exampleID, expressionStart, expressionEnd, 'subgraph-' + subgraphId + '-node-misuse');

            console.log('remove span (skeleton)' + exampleID);

            // console.log('length = ' + $("#exampleID12 pre").length);
          });
        }
      } 
      
    } else {
      if (!_.isEmpty(obj[nodeText])) {
        var expressionStart = obj[nodeText]['expressionStart'];
        var expressionEnd = obj[nodeText]['expressionEnd'];
        
        if (Object.keys(skeleton[node]).includes('allSubgraphs')) {
          skeleton[node]['subgraphIds'].forEach(function(subgraphId){

            var fullSubgraph = Subgraphs.findOne({_id: subgraphId}); 

            if (fullSubgraph== undefined) return '';
            // console.log('subgraph', subgraphId, fullSubgraph);
            // var cssClassName = 'subgraph-' + subgraphId + '-node';
            var cssClassName = 'subgraph-0-node';
            if (fullSubgraph.isPattern === true) {
              cssClassName += '-correct';
            } else if (fullSubgraph.isPattern === false) {
              cssClassName += '-misuse';
            } else {
              cssClassName += '';
            }
            
            addSpan(exampleID, obj['clusterContext'], expressionStart, expressionEnd, cssClassName);

            
            // console.log('add span (skeleton)' + exampleID + ' ' + expressionStart + ' ' + expressionEnd + ' ' + cssClassName);
            
            // console.log('adding spans', exampleID, cssClassName);
          });
        }
      } 
    }
  }

  if (explanation.length > 0) {
    $('#exampleID'  + obj['exampleID'] + clusterContext  + ' .match-explanation').removeClass('hidden').html('NOT&nbsp;' + explanation);
  } else {
    $('#exampleID'  + obj['exampleID'] + clusterContext  + ' .match-explanation').addClass('hidden');
  }
  
  if (clusterContext != '-labelled') {
    return;
  }
  buildSummary();
  var summary = Session.get('summary');
 
  for (let summaryNodeKey of Object.keys(summary)) {

    var summaryNode = summary[summaryNodeKey];
    var elementToMatch = summaryNode['text'];
    if (obj[elementToMatch] === undefined) {
      continue;
    }

    if (skeleton[summaryNodeKey] && skeleton[summaryNodeKey]['checked']) {
      continue;
    }

    var expressionStart = obj[elementToMatch]['expressionStart'];
    var expressionEnd = obj[elementToMatch]['expressionEnd'];

    // add css style (copied from tutorons)
    var range = parent.window.document.createRange();
    var node = parent.window.document.querySelector('#exampleID' + parseInt(exampleID) + '-' + obj['clusterContext']);
    if (node != null) {
      var textNodes = getTextDescendants(node);
      var textRanges = getRangeInText(textNodes, expressionStart, expressionEnd + 1);
      try {
        range.setStart(textRanges.start.node, textRanges.start.offset);
        range.setEnd(textRanges.end.node, textRanges.end.offset);

        // Transfer found terms into a span
        var contents = range.extractContents();
        var span = this.window.document.createElement('span');
        // $(span).addClass('code-summary');
        span.appendChild(contents);
        // console.log('setting code summary color')

        $(span).css('background-color', 'hsla(' + summaryNode['color'].toString() + ',60%, 60%, 0.7)');
        range.insertNode(span);

        
        // console.log('add span (summary)' + exampleID + ' ' + expressionStart + ' ' + expressionEnd + ' ' + 'code-summary');
      } catch (e) {
        console.log('error when rendering summary', e);
      }
      
    }
  }
  // Session.set('skeleton', skeleton);
  // Session.set('skeleton', skeleton);
}

var computeAverageStart = function() {

  // focal API
  var focalAPI = getFocalAPI();



  if (!_.isEmpty(Session.get('averageStarts'))) { 
    return Session.get('averageStarts');
  }
  var examples = fetchShortestExamples();

  var averageStart = {};
  var totalSeen = {}
  examples.forEach(function(example) {
    // if (example[focalAPI] == undefined) return;
    // var focalAPIPosition = example[focalAPI]['expressionStart'];
    Object.keys(example).forEach(function(key) {
      if (key == 'exampleID' || key == 'label') return;
      if (!example[key]) return;
      if (example[key] == undefined) return;
      if (averageStart[key] == undefined) {
        averageStart[key] = 0;
      }
      averageStart[key] += example[key]['expressionStart'];
      totalSeen[key] = (totalSeen[key] || 0) + 1;
    });
  });

  Object.keys(averageStart).forEach(function(key) {
    averageStart[key] = averageStart[key] / totalSeen[key];
  }
  );
  
  Session.set('averageStarts', averageStart);
  return averageStart;
}

var sortSingleNodes = function(subgraphs) {

  var inDegree = {};
  var nodeContainedInSubgraph = {};
  var isPreview = {};

  subgraphs.forEach(function(subgraph) {
    Object.keys(subgraph.adjlist).forEach(function(key) {
      if (key == '') return;
      inDegree[key] = 0;
      if (nodeContainedInSubgraph[key] == undefined) {
        nodeContainedInSubgraph[key] = [];
      }
      nodeContainedInSubgraph[key].push(subgraph._id);

      if (nodeContainedInSubgraph[key].length > 1 ) {
        // alert('should not happen anymoer')
        console.log('uhhhhh. Multiple subgraphs have the same node (text). this shouldnt happen anymore. ' + key)
        // alert('here!')
        nodeContainedInSubgraph[key] = [nodeContainedInSubgraph[key][0]];
      }

      isPreview[key] = subgraph.preview;
    });
  });

  subgraphs.forEach(function(subgraph) {
    for (const edge of subgraph.edges) {
      if (edge['to'] == '') return;
      inDegree[edge['to']] += 1;
    }
  });

  const queue = [];
  subgraphs.forEach(function(subgraph) {
    Object.keys(subgraph.adjlist).forEach(function(key) {
      if (inDegree[key] == 0) queue.push(  key );
    });
  });

  
  // console.log(inDegree);

  var results = [];
  var alreadyInResults = {};
  while (queue.length) {
    const from = queue.shift();
    if (alreadyInResults[from]) continue;
    results.push(  {'subgraphIds' : nodeContainedInSubgraph[from ],  'text': from.replace('__', '.') , 'isPreview': isPreview[from]} ); // convert names back to human-readable form
    alreadyInResults[from] = true;
    
    subgraphs.forEach(function(subgraph) {
      if (subgraph.adjlist[from]  == undefined) return;
      subgraph.adjlist[from].forEach(function(edge) {
        var to = edge.to;

        if (to == '') return;

        inDegree[to] -= 1;

        if (inDegree[to] == 0) queue.push(to );

      });
    });
  }

  if (results.length != Object.keys(inDegree).length) {
    console.log('no topological sort. add to the queue the node with smallest in degree ');

    // just order them in inDegree
    var sorted = Object.keys(inDegree).sort(function(a, b) {
      return inDegree[a] - inDegree[b];
    }
    );
    console.log('sorted by indegree',sorted);
    
    var result = [];
    sorted.forEach(function(key) {
      result.push(  {'subgraphIds' : nodeContainedInSubgraph[key ],  'text': key.replace('__', '.') , 'isPreview': isPreview[key] } ); // convert names back to human-readable form
    });
    return result;

  }
  return results;

}

var sortNodesFromEdges = function(subgraphs) {
  var allEdges = [];
  var nodeContainedInSubgraph = {};
  var inDegree = {};
  subgraphs.forEach(function(subgraph) {
    allEdges = allEdges.concat(subgraph.edges);
  });
  subgraphs.forEach(function(subgraph) {
    subgraph.edges.forEach(function(edge) {
      [edge.from, edge.to].forEach(function(key) {
        if (key == '') return;
        
        if (nodeContainedInSubgraph[key] == undefined) {
          nodeContainedInSubgraph[key] = [];
        }
        nodeContainedInSubgraph[key].push(subgraph._id);

        if (nodeContainedInSubgraph[key].length > 1 ) {
          nodeContainedInSubgraph[key] = [nodeContainedInSubgraph[key][0]];
        }
      });

      // isPreview[key] = subgraph.preview;
    });
  });



  var nodes = {};
  var nodeLabels = [];
  subgraphs.forEach(function(subgraph) {
    subgraph.edges.forEach(function(edge) {
      nodeLabels.push(edge.from);
      nodeLabels.push(edge.to);
    });
  });
  nodeLabels = nodeLabels.filter(item => item != '').filter(item => !item.includes('pseudo'));
  nodeLabels.forEach(function(node) {
    nodes[node] = {'text' : node, 'subgraphIds': nodeContainedInSubgraph[node]};
  });
  subgraphs.forEach(function(subgraph) {
    subgraph.edges.forEach(function(edge) {
      if ((edge.label === '(para)' && edge.from.includes("(")) || edge.label === '(def)') {
        var fromNode = nodes[edge.from];

        if (fromNode.children === undefined) {
          fromNode.children = [];
          fromNode.childrenEdge = {};
        }

        // if edge.from is not metod call, add
        if (!edge.to.includes('(')) {
          // add if fromNode.children does not already contain edge.to
          if (!_.contains(fromNode.children, nodes[edge.to])) {

            // and if edge.to doesn't contain children of its own
            // if (_.isEmpty(nodes[edge.to].children)) {
              var descendentNode = structuredClone(nodes[edge.to]);
              fromNode.children.push(descendentNode); 
              fromNode.childrenEdge[edge.to] = edge.label;


            // }
          }
        }
      } else if ( (edge.label === '(para)') || edge.label === '(recv)') {
        var to = nodes[edge.to];
        if (to.children === undefined) {
          to.children = [];
          to.childrenEdge = {};
        }

        // if edge.from is not metod call, add
        // if (!edge.to.includes('(')) {
        // add if to.children does not already contain edge.from
        if (!_.contains(to.children, nodes[edge.from])) {
          // if (_.isEmpty(nodes[edge.from].children)) {
            var descendentNode = structuredClone(nodes[edge.from]);

            to.children.push(descendentNode);
            to.childrenEdge[edge.from] = edge.label;


          // }
          // to.children.push(nodes[edge.from]);
        }
        // }
      }
    });
  });
  // return nodes not a children of another node
  var children = [];
  Object.keys(nodes).forEach(function(key) {
    if (nodes[key].children) {
      nodes[key].children.forEach(function(child) {
        children.push(child.text);
      });
    }
  });

  return _.filter(nodes, function(node) {
    return !_.contains(children, node.text);
  });

  // return nodes;

}

var sortNodes = function(subgraphs) {
  // order by "order", "throw", "finally", "sel" edges
  var edges = subgraphs.flatMap(function(subgraph) {
    return subgraph.edges;
  });

  var nodeContainedInSubgraph = {};
  var isPreview = {};

  subgraphs.forEach(function(subgraph) {
    Object.keys(subgraph.adjlist).forEach(function(key) {
      if (key == '') return;
      if (nodeContainedInSubgraph[key] == undefined) {
        nodeContainedInSubgraph[key] = [];
      }
      nodeContainedInSubgraph[key].push(subgraph._id);

      if (nodeContainedInSubgraph[key].length > 1 ) {
        nodeContainedInSubgraph[key] = [nodeContainedInSubgraph[key][0]];
      }

      isPreview[key] = subgraph.preview;
    });
  });

  var nodes = {};
  var nodeLabels = [];
  subgraphs.forEach(function(subgraph) {
    subgraph.edges.forEach(function(edge) {
      nodeLabels.push(edge.from);
      nodeLabels.push(edge.to);
    });
  });

  var averageStarts = computeAverageStart();
  nodeLabels.forEach(function(node) {
    nodes[node] = {'text' : node, 'subgraphIds': nodeContainedInSubgraph[node], 'isPreview': isPreview[node] };

    nodes[node]['start'] = averageStarts[node] || 0;
  });

  // nest "para", "recv", "def" nodes
  subgraphs.forEach(function(subgraph) {
    subgraph.edges.forEach(function(edge) {
      if ((edge.label === '(para)' && edge.from.includes("(")) || edge.label === '(def)') {
        var fromNode = nodes[edge.from];

        if (fromNode.children === undefined) {
          fromNode.children = [];
          fromNode.childrenEdge = {};
        }

        // if edge.from is not metod call, add
        if (!edge.to.includes('(')) {
          // add if fromNode.children does not already contain edge.to
          if (!_.contains(fromNode.children, nodes[edge.to])) {

            // and if edge.to doesn't contain children of its own
            // if (_.isEmpty(nodes[edge.to].children)) {
              var descendentNode = structuredClone(nodes[edge.to]);
              fromNode.children.push(descendentNode); 
              fromNode.childrenEdge[edge.to] = edge.label;


            // }
          }
        }
      } else if ( (edge.label === '(para)') || edge.label === '(recv)') {

        var calling = nodes[edge.to];
        if (!edge.to.includes('(') && !edge.to.includes('<')) {
          calling = nodes[edge.from];
        }
        if (calling.children === undefined) {
          calling.children = [];
          calling.childrenEdge = {};
        }

        // if edge.from is not metod call, add
        // if (!edge.to.includes('(')) {
        // add if to.children does not already contain edge.from
        if (!_.contains(calling.children, nodes[edge.from])) {
          // if (_.isEmpty(nodes[edge.from].children)) {
            var descendentNode = structuredClone(nodes[edge.from]);

            calling.children.push(descendentNode);
            calling.childrenEdge[edge.from] = edge.label;


          // }
          // to.children.push(nodes[edge.from]);
        }
        // }
      }
    });
  });
  console.log('sortNodes');
  console.log(nodes);
  

  edges.forEach(function(edge) {
    if (edge.label === '(order)' || edge.label === '(throw)' || edge.label === '(finally)' || edge.label === '(sel)') {
      
      if (nodes[edge.from].outgoingEdges === undefined) {
        nodes[edge.from].outgoingEdges = [];
      }
      nodes[edge.from].outgoingEdges.push(edge);

    }
  });

  
  var startNodes = _.uniq(nodeLabels)
  // .filter(function(node) {
  //   return   (node.includes('<') || node.includes('('));
  // });

  // order them by the average start position
  startNodes = startNodes.sort(function(a, b) {
    var aStart = averageStarts[a];
    var bStart = averageStarts[b];
    if (aStart < bStart) {
      return -1;
    } else if (aStart > bStart) {
      return 1;
    } else {
      return 0;
    }
  });

  // console.log(startNodes)

  var focalAPI = getFocalAPI();
  // split startNodes into before and after focal node
  var methodsPrecedingFocal = startNodes.slice(0, startNodes.indexOf(focalAPI))
  .filter(function(nodeLabel) {
    return !nodeLabel.includes('<');
  })
  .map(function(nodeLabel) {
    return nodes[nodeLabel];
  });

  var controlPrecedingFocal = startNodes.slice(0, startNodes.indexOf(focalAPI))
  .filter(function(nodeLabel) {
    return nodeLabel.includes('<') && !nodeLabel.includes('<init>') && !nodeLabel.includes('<cast>');
  })
  .map(function(nodeLabel) {
    return nodes[nodeLabel];
  }
  );

  var decl = startNodes
  .filter(function(nodeLabel) {
    return nodeLabel.includes('<init>') || nodeLabel.includes('<cast>');
  })
  .map(function(nodeLabel) {
    return nodes[nodeLabel];
  });
  var methodsFollowingFocal = startNodes.slice(startNodes.indexOf(focalAPI) + 1, startNodes.length)
  .filter(function(nodeLabel) {
    return !nodeLabel.includes('<') ;
  })
  .map(function(nodeLabel) {
    return nodes[nodeLabel];
  }
  );

  var controlFollowingFocal = startNodes.slice(startNodes.indexOf(focalAPI) + 1, startNodes.length)
  .filter(function(nodeLabel) {
    return nodeLabel.includes('<') && !nodeLabel.includes('<init>') && !nodeLabel.includes('<cast>');
  })
  .map(function(nodeLabel) {
    return nodes[nodeLabel];
  });


  // some pseudonodes
  var before = {'text' : 'Pre method call' , 'subgraphIds': ['pseudo-bef'], 'isPreview': false, 'children': methodsPrecedingFocal, 'contextColor': '360'};
  var before_control = {'text' : 'Control flow' , 'subgraphIds': ['pseudo-bef-control'], 'isPreview': false, 'children': controlPrecedingFocal, 'contextColor': '160' };
  var decls = {'text' : 'Constructors' , 'subgraphIds': ['pseudo-bef-decl'], 'isPreview': false, 'children': decl, 'contextColor': '120'};
  var focal = nodes[focalAPI] ? {'text' : focalAPI, 'subgraphIds': ['pseudo-focal'], 'isPreview': false, 'children': nodes[focalAPI]['children'] , 'contextColor': '210'}
           : {'text' : focalAPI, 'subgraphIds': ['pseudo-focal'], 'isPreview': false, 'children': [], 'contextColor': '210'};
  var after = {'text' : 'Post method call ' , 'subgraphIds': ['pseudo-aft'], 'isPreview': false, 'children': methodsFollowingFocal, 'contextColor': '220' };
  var after_control = {'text' : 'Control flow ' , 'subgraphIds': ['pseudo-aft-control'], 'isPreview': false, 'children': controlFollowingFocal, 'contextColor': '160' };
  // var after_decl = {'text' : 'declaration ' , 'subgraphIds': ['pseudo-aft-decl'], 'isPreview': false, 'children': declFollowingFocal, 'contextColor': 'pseudo-aft-decl' };

  var result = [decls, before_control, before,
     //focal, 
     after_control, after];
  if (Session.get('view') == 'labelled' || startNodes.indexOf(focalAPI) == -1) {
    var methodCall = {'text' : 'Method calls' , 'subgraphIds': ['pseudo-bef'], 'isPreview': false, 'children': _.uniq(methodsPrecedingFocal.concat(methodsFollowingFocal)), 'contextColor': '33' };
    result = [decls, methodCall, before_control,];
      // focal ];
  } 


  // propagate "contextColor" property to children
  var propagateContext = function(node) {
    if (node.children) {

      // sort the children alphabetically
      node.children = node.children.sort(function(a, b) {
        if (a.text < b.text) {
          return -1;
        } else if (a.text > b.text) {
          return 1;
        } else {
          return 0;
        }
      });

      // for loop over the children
      for (var i = 0; i < node.children.length; i++) {
        var child = node.children[i];

        var text = node.subgraphIds[0].includes('pseudo') ? node.subgraphIds[0] : node.text;

        if (node.childrenEdge) {
          child.parent = node.parent ? node.parent.concat([text + '{' + node.childrenEdge[child.text] + '}']) : [text + '{' + node.childrenEdge[child.text] + '}'];
        } else {
          child.parent = node.parent ? node.parent.concat([text]) : [text];
        }
        // child.parent = node.parent ? node.parent.concat([text ]) : [text ];

        child.contextColor = parseInt(node.contextColor);
        propagateContext(child);
      }
    }
  };
  result.forEach(propagateContext);

  return result;
  
  // var sortedNodes = [];
  // var visitedNodes = {};
  // var visitNode = function(nodeLabel) {

  //   if (visitedNodes[nodeLabel]) {
  //     return;
  //   }
  //   // console.log('visitNode', nodeLabel, nodes[nodeLabel])
    
  //   visitedNodes[nodeLabel] = true;
  //   sortedNodes.push(nodes[nodeLabel]);

  //   if (nodes[nodeLabel].outgoingEdges) {
    
  //     nodes[nodeLabel].outgoingEdges.forEach(function(edge) {
  //       visitNode(edge.to);
  //     });
  //   }
    
  // };
  // startNodes.forEach(visitNode);

  // return sortedNodes;

  // return startNodes.map(function(nodeLabel) {
  //   return nodes[nodeLabel];
  // }
  // );
  
};

// from https://stackoverflow.com/questions/73779689/combine-2-colors-in-javascript
function colorChannelMixer(colorChannelA, colorChannelB, amountToMix) {
  var channelA = colorChannelA * amountToMix;
  var channelB = colorChannelB * (1 - amountToMix);
  return Math.round(channelA + channelB);
}
function colorMixer(rgbA, rgbB, amountToMix) {
  var r = colorChannelMixer(rgbA[0], rgbB[0], amountToMix);
  var g = colorChannelMixer(rgbA[1], rgbB[1], amountToMix);
  var b = colorChannelMixer(rgbA[2], rgbB[2], amountToMix);
  return [r, g, b]
}


function combine(cssClasses) {
  if (cssClasses.length == 1) {
    return $('.' + cssClasses[0]).css('backgroundColor');
  }
  var backgroundColors = cssClasses.map(function(cssClass) {
    // console.log(cssClass,);
    var backgroundColors = $('.' + cssClass).css('backgroundColor');
   
    // console.log(cssClass + ' .... ' +  backgroundColors);
    if (backgroundColors == undefined) return undefined;
    if (backgroundColors.indexOf('rgb(') == -1) {
      console.log('hey!');
    }

    return backgroundColors
    .split('rgb(')[1]
    .split(')')[0]
    .split(',')
    .slice(0,3)
    .map(function(item){ return parseInt(item); })
  }).filter(function(item) { return item != undefined; });
  return 'rgb(' + colorMixer(backgroundColors[0], backgroundColors[1], 0.5).join(',') + ')';
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

Template.example.onRendered(function() {
    // $('#'+this.data.exampleID + ' pre code').each(function(i, block) {
    //   hljs.highlightBlock(block);
    // });

    var that = this;
    Meteor.defer(function() {
    
      buildSkeleton();
      hljs.highlightBlock(that.find('code'), { language: 'java' });
      render(that['data']);
    });

    // for (let blockname of blocknames){
    //   if (blockname==='initialization' && !_.isEmpty(this['data']['initializationStart'])) {
    //     var expressionStart = this['data']['initializationStart'];
    //     var expressionEnd = this['data']['initializationEnd'];
    //     addMultipleSpans(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==='try' && this['data']['tryExpressionStart'] !== -1) {
    //     var expressionStart = this['data']['tryExpressionStart'];
    //     var expressionEnd = this['data']['tryExpressionEnd'];
    //     addSpan(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==='configuration' && !_.isEmpty(this['data']['configurationStart'])) {
    //     var expressionStart = this['data']['configurationStart'];
    //     var expressionEnd = this['data']['configurationEnd'];
    //     addMultipleSpans(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==="guardCondition" && this['data']['guardExpressionStart'] !== -1 ) {
    //     var expressionStart = this['data']['guardExpressionStart'];
    //     var expressionEnd = this['data']['guardExpressionEnd'];
    //     addSpan(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==='focalAPI' && this['data']['focalAPIStart'] !== -1 ) {
    //     var expressionStart = this['data']['focalAPIStart'];
    //     var expressionEnd = this['data']['focalAPIEnd'];
    //     addSpan(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==="checkType" && this['data']['followUpCheckExpressionStart'] !== -1) {
    //     var expressionStart = this['data']['followUpCheckExpressionStart'];
    //     var expressionEnd = this['data']['followUpCheckExpressionEnd'];
    //     addSpan(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==="use" && !_.isEmpty(this['data']['useStart'])) {
    //     var expressionStart = this['data']['useStart'];
    //     var expressionEnd = this['data']['useEnd'];
    //     addMultipleSpans(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==="exceptionType" && this['data']['catchExpressionStart'] !== -1 ) {
    //     var expressionStart = this['data']['catchExpressionStart'];
    //     var expressionEnd = this['data']['catchExpressionEnd'];
    //     addSpan(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==="exceptionHandlingCall" && !_.isEmpty(this['data']['exceptionHandlingCallStart'])) {
    //     var expressionStart = this['data']['exceptionHandlingCallStart'];
    //     var expressionEnd = this['data']['exceptionHandlingCallEnd'];
    //     addMultipleSpans(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==="finally" && this['data']['finallyExpressionStart'] !== -1 ) {
    //     var expressionStart = this['data']['finallyExpressionStart'];
    //     var expressionEnd = this['data']['finallyExpressionEnd'];
    //     addSpan(exampleID,expressionStart,expressionEnd,blockname);
    //   } else if (blockname==="cleanUpCall" && !_.isEmpty(this['data']['cleanUpCallStart'])) {
    //     var expressionStart = this['data']['cleanUpCallStart'];
    //     var expressionEnd = this['data']['cleanUpCallEnd'];
    //     addMultipleSpans(exampleID,expressionStart,expressionEnd,blockname);
    //   } 
    // }
});

function simulateProgress() {
  var progressBar = document.querySelector('.progress');
  // remove hidden class
  progressBar.classList.remove('hidden');
  var width = 0;
  var interval = setInterval(increaseWidth, 50);

  function increaseWidth() {
    if (width >= 100) {
      clearInterval(interval);
      // add hidden class again
      progressBar.classList.add('hidden');
    } else {
      width++;
      progressBar.style.width = width + '%';
    }
  }
}

function showStatus(status) {
  showLoadingText();

  var statusElement = document.querySelector('.status-box');
  statusElement.innerHTML = status;
  statusElement.classList.remove('hidden');
  statusElement.classList.add('visible');

  if (status == '') {
    statusElement.classList.remove('visible');
    statusElement.classList.add('hidden');
  }
}

function showLoadingText() {
  var loadingElement = document.querySelector('.loading-text');
  loadingElement.classList.remove('hidden');
  loadingElement.classList.add('visible');
}

function hideLoadingText() {
  var loadingElement = document.querySelector('.loading-text');
  loadingElement.classList.remove('visible');
  loadingElement.classList.add('hidden');
}


var colorSubgraphNodeSelection = function(subgraphs) {
  var subgraphsStr = subgraphs.join('-');

    // if (subgraphs.length > 1) {
    var cssClasses = subgraphs.map(function(subgraphStr) {
      var one = Subgraphs.findOne({ _id: subgraphStr });
      var isPattern = one.isPattern;
      if (isPattern === true) {
        return 'subgraph-' + subgraphStr + '-node-correct';
      } else if (isPattern === false) {
        return 'subgraph-' + subgraphStr + '-node-misuse';
      } else {
        return 'subgraph-' + subgraphStr + '-node';
      }
    });
        
    
    $('.subgraph-node-selection-' + subgraphsStr).css('background-color', combine(cssClasses))
    
    // console.log('setting color of .subgraph-node-selection-' + subgraphsStr + " of " + cssClasses);
    // } 
}

var hasSelectedPatternInPalette = function() {
    var skeleton = Session.get('skeleton');

  // if there is nothing in the skeleton, then the mode is disabled
  if (_.isEmpty(skeleton)) return false;

  var hasChecked = false;
  Object.keys(skeleton).forEach(function(key) {
    if (skeleton[key].checked) {
      hasChecked = true;
    }
  }
  );
  if (!hasChecked) return false;

  return true;
}

var entropy = function(subgraphLabelledPositiveMatches, subgraphLabelledNegativeMatches, defaultValue) {
  var numPositives = subgraphLabelledPositiveMatches ? subgraphLabelledPositiveMatches.length : 0;
  var numNegatives = subgraphLabelledNegativeMatches ? subgraphLabelledNegativeMatches.length : 0;
  
  var total = numPositives + numNegatives;
  if (total === 0) return defaultValue;

  var pPos = numPositives / total;
  var pNeg = numNegatives / total;

  var entropy = 0;
  if (pPos !== 0) {
    entropy -= pPos * Math.log2(pPos);
  }
  if (pNeg !== 0) {
    entropy -= pNeg * Math.log2(pNeg);
  }
  
  return entropy;
}

var examplesMatchedByAAndNotB = function() {
  var skeleton = Session.get('skeleton');

  var subgraphA = Session.get('whatIfSubgraphA');
  var  subgraphB = Session.get('whatIfSubgraphB');

  if (!subgraphA || !subgraphB) return [];

  var selectorWithAdditionalSubgraphA = computeSelectorWithAdditionalSubgraphByText(skeleton, subgraphA);


  var selectorWithAdditionalSubgraphB = computeSelectorWithAdditionalSubgraphByText(skeleton, subgraphB);

  var examplesA = fetchShortestExamples(constructSelectorToFilterBaggedPatterns((selectorWithAdditionalSubgraphA)));
  var examplesB = fetchShortestExamples(constructSelectorToFilterBaggedPatterns((selectorWithAdditionalSubgraphB)));

  var examplesBSet = new Set(examplesB.map(function(example) { return example._id; }));
  var examplesAAndNotB = examplesA.fetch().filter(function(example) { return !examplesBSet.has(example._id); });
  
  return examplesAAndNotB;
}
var examplesMatchedByBAndNotA = function() {
  var skeleton = Session.get('skeleton');

  var subgraphA = Session.get('whatIfSubgraphA');
  var  subgraphB = Session.get('whatIfSubgraphB');

  if (!subgraphA || !subgraphB) return [];

  var selectorWithAdditionalSubgraphA = computeSelectorWithAdditionalSubgraphByText(skeleton, subgraphA);


  var selectorWithAdditionalSubgraphB = computeSelectorWithAdditionalSubgraphByText(skeleton, subgraphB);

  var examplesA = fetchShortestExamples(constructSelectorToFilterBaggedPatterns((selectorWithAdditionalSubgraphA)));
  var examplesB = fetchShortestExamples(constructSelectorToFilterBaggedPatterns((selectorWithAdditionalSubgraphB)));

  var examplesASet = new Set(examplesA.map(function(example) { return example._id; }));
  var examplesBAndNotA = examplesB.fetch().filter(function(example) { return !examplesASet.has(example._id); });
  
  return examplesBAndNotA;
}

var examplesThatBoostsBOverA = function() {
  var skeleton = Session.get('skeleton');

  var subgraphA = Session.get('whatIfSubgraphA');
  var subgraphB = Session.get('whatIfSubgraphB');

  if (!subgraphA || !subgraphB) return [];

  var subgraphAScore =  Session.get('whatIfSubgraphAScore');

  // looking for unlabelled examples that are matched by B (and maybe also A, but maybe not)
  var selectorWithAdditionalSubgraphB = computeSelectorWithAdditionalSubgraphByText(skeleton, subgraphB);
  var examplesB = fetchShortestExamples(constructSelectorToFilterBaggedPatterns((selectorWithAdditionalSubgraphB)));

  // also for unlabelled examples matched by A
  var selectorWithAdditionalSubgraphA = computeSelectorWithAdditionalSubgraphByText(skeleton, subgraphA);
  var examplesA = fetchShortestExamples(constructSelectorToFilterBaggedPatterns((selectorWithAdditionalSubgraphA)));


  return examplesA.fetch().filter(function(example) { 
    return !example.label || example.label == '?'; 
  }).slice(0,5).concat(examplesB.fetch().filter(function(example) {
    return !example.label || example.label == '?';
    }).slice(0,5));

  // 

  // var selectorWithAdditionalSubgraphA = computeSelectorWithAdditionalSubgraphByText(skeleton, subgraphA);
}


var computeMatchingPositiveExamples = function() {
  var skeleton = Session.get('skeleton');

  var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
  
  // find examples where label is 'positive' and 
  // the example matches the selector
  if (selector['$and']) {
    var mergedSelectorConjunction = selector['$and'].concat({'label': 'positive'});
    var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
  } else {
    var mergedSelector = {'label': 'positive'};
  }
  var currentCount = fetchShortestExamples(mergedSelector).count();
  return currentCount;
  
}

var computeMatchingNegativeExamples = function() {
  var skeleton = Session.get('skeleton');

  var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
  
  // find examples where label is 'negative' and 
  // the example matches the selector
  if (selector['$and']) {
    var mergedSelectorConjunction = selector['$and'].concat({'label': 'negative'});
    var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
  } else {
    var mergedSelector = {'label': 'negative'};
  }
  var currentCount = fetchShortestExamples(mergedSelector).count();
  return currentCount;
  
}

var computeMatchingUnlabelledExamples = function() {

  var skeleton = Session.get('skeleton');

  var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
  
  // find examples where label is 'negative' and 
  // the example matches the selector
  if (selector['$and']) {
    var mergedSelectorConjunction = selector['$and'].concat({'label': '?'});
    var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
  } else {
    var mergedSelector = {'label': '?'};
  }
  var currentCount = fetchShortestExamples(mergedSelector).count();

  return currentCount;
}

var computeUnmatchingPositiveExamples = function() {
  var skeleton = Session.get('skeleton');

  var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
  
  // find examples where label is 'positive' and 
  // the example matches the selector
  if (selector['$and']) {
    var mergedSelectorConjunction = selector['$and'].concat({'label': 'positive'});
    var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
  } else {
    var mergedSelector = {'label': 'positive'};
  }
  var matchingPositiveExamples = fetchShortestExamples(mergedSelector).count();

  // all positive
  var allPositiveExamples = fetchShortestExamples({'label': 'positive'}).count();

  var currentCount = allPositiveExamples - matchingPositiveExamples;
  return currentCount;
}

var computeUnmatchingNegativeExamples = function() {
  var skeleton = Session.get('skeleton');

  var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
  
  // find examples where label is 'negative' and 
  // the example matches the selector
  if (selector['$and']) {
    var mergedSelectorConjunction = selector['$and'].concat({'label': 'negative'});
    var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
  } else {
    var mergedSelector = {'label': 'negative'};
  }
  var matchingNegativeExamples = fetchShortestExamples(mergedSelector).count();

  // all negative
  var allNegativeExamples = fetchShortestExamples({'label': 'negative'}).count();

  var currentCount = allNegativeExamples - matchingNegativeExamples;
  return currentCount;
}

var computeUnmatchingUnlabelledExamples = function() {
  var skeleton = Session.get('skeleton');

  var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
  
  // find examples where label is 'negative' and 
  // the example matches the selector
  if (selector['$and']) {
    var mergedSelectorConjunction = selector['$and'].concat({'label': '?'});
    var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
  } else {
    var mergedSelector = {'label': '?'};
  }
  var matchingUnlabelledExamples = fetchShortestExamples(mergedSelector).count();

  // all unlabelled
  var allUnlabelledExamples = fetchShortestExamples({'label': '?'}).count();

  var currentCount = allUnlabelledExamples - matchingUnlabelledExamples;
  return currentCount
}

Template.node.nodeRendered = function(subgraphs) {
  if (subgraphs == undefined) return;

  Meteor.defer(function () {

    // colorSubgraphNodeSelection(subgraphs);
  });
}

Template.frequentNode.nodeRendered = function(subgraphs) {
  if (subgraphs == undefined) return;
  Meteor.defer(function () {

    // colorSubgraphNodeSelection(subgraphs);
  });
}

/*
 * Register the helper functions for the body template
 */
Template.body.helpers({
  
  filteredExamples() {
    // build the skeleton of the selector
    buildSkeleton();
    var skeleton = Session.get('skeleton');

    var examples = filterByView(skeleton).fetch();

    examples = examples.slice(0, 30);
    // examples.forEach(function(example) {
    //   render(example);
    // });
    console.log('[filteredExamples] returning', examples.length, 'examples')

    // add clusterContext to the examples
    examples.forEach(function(example) {
      var clusterContext = 'filtered';

      example.clusterContext = clusterContext;

    });
    // take just 30
    return examples;
  },
  labelledExamples() {

    var examples = Examples.find({label: {$ne: '?'}}, {sort: {label: -1}}).fetch();
    examples.forEach(function(example) {
      example.clusterContext = 'labelled';
      render(example);
    });
    return examples;
  },
  labelledPositiveExamples() {
    var examples = Examples.find({label: 'positive'}, {sort: {label: -1}}).fetch();
    examples.forEach(function(example) {
      example.clusterContext = 'labelled';
      render(example);
    });
    return examples;
  },
  labelledNegativeExamples() {
    var examples = Examples.find({label: 'negative'}, {sort: {label: -1}}).fetch();
    examples.forEach(function(example) {
      example.clusterContext = 'labelled';
      render(example);
    });
    return examples;
  },
  showExamplesMatchedByAAndNotB() {
    var examples = examplesMatchedByAAndNotB();
    examples.forEach(function(example) {
      example.clusterContext = 'whatIf';
      example.additionalNode = Session.get('whatIfSubgraphA');
      Meteor.defer(function() {
        render(example);
        });
    });
    examples.sort(function(a, b) {
      if (a['prelabelled'] && !b['prelabelled']) {
        
        return -1;
      }
      if (!a['prelabelled'] && b['prelabelled']) {
        return 1;
      }

      if (a['prelabelled'] && b['prelabelled']) {
        // sort by their label, first show positive, then negative, then '?'
        if (a['label'] === 'positive' && b['label'] !== 'positive') {
          return -1;
        }
        if (a['label'] !== 'positive' && b['label'] === 'positive') {
          return 1;
        }
        if (a['label'] === 'negative' && b['label'] !== 'negative') {
          return -1;
        }
        if (a['label'] !== 'negative' && b['label'] === 'negative') {
          return 1;
        }
        return 0;
      } else {
        // sort by exampleID
        return  b['exampleID'] - a['exampleID'];
      }
    });
    return examples;
  },
  showExamplesMatchedByBAndNotA() {
    var examples = examplesMatchedByBAndNotA();
    // render the examples
    examples.forEach(function(example) {
      example.clusterContext = 'whatIf';
      example.additionalNode = Session.get('whatIfSubgraphB');
      Meteor.defer(function() {
        render(example);
      });
    });
    examples.sort(function(a, b) {
      if (a['prelabelled'] && !b['prelabelled']) {
        
        return -1;
      }
      if (!a['prelabelled'] && b['prelabelled']) {
        return 1;
      }

      if (a['prelabelled'] && b['prelabelled']) {
        // sort by their label, first show positive, then negative, then '?'
        if (a['label'] === 'positive' && b['label'] !== 'positive') {
          return -1;
        }
        if (a['label'] !== 'positive' && b['label'] === 'positive') {
          return 1;
        }
        if (a['label'] === 'negative' && b['label'] !== 'negative') {
          return -1;
        }
        if (a['label'] !== 'negative' && b['label'] === 'negative') {
          return 1;
        }
        return 0;
      } else {
        // sort by exampleID
        return  b['exampleID'] - a['exampleID'];
      }
    });
    return examples;
  },
  showExamplesThatBoostsBOverA() {
    return examplesThatBoostsBOverA();
  },
  emptyText(collection) {
    return collection.length == 0 ? 'None' : '';
  } ,
  
  whatIfSubgraphA() {
    return Session.get('whatIfSubgraphA');
  },
  whatIfSubgraphB() {
    return Session.get('whatIfSubgraphB');
  },

  whatIfSubgraphAColor() {
    return Session.get('whatIfSubgraphAColor');
  },
  whatIfSubgraphBColor() {
    return Session.get('whatIfSubgraphBColor');
  },
  clusteredExamples() {
    return determineChoicesOfCandidates(patternGrowingCandidateNodes, computeSelectorWithAdditionalSubgraphByText);
  },
  

  clusteredExamplesForAlternative() {
    return determineChoicesOfCandidates(alternativeSubgraphsCandidateNodes, computeSelectorWithOnlySubgraphByText);
    
  },

  clusteredExamplesFollowingConfig() {
    var config = Config.findOne({});
    if (config.showStreamlined) {
      return determineChoicesOfCandidates(patternGrowingCandidateNodes, computeSelectorWithAdditionalSubgraphByText);
    } else {
      return determineChoicesOfCandidates(alternativeSubgraphsCandidateNodes, computeSelectorWithAdditionalSubgraphByText);// computeSelectorWithOnlySubgraphByText);
    }

  },

  discriminativeSubgraphsSkeleton() {
    var subgraphs = Subgraphs.find({ '$and' : 
      [
        {'discriminative': true},  
        {'hidden': {'$ne': true}},
        {'$or': [{ 'bags': { '$exists': false } }, { 'bags': { $eq: null } }]}
      ]
    }).fetch();

    var headerText = {};
    var hastoolFeedback = {};
    var headerCount = 0;

    // console.log('[discriminativeSubgraphsSkeleton] subgraphs')
    // console.log(subgraphs);

    // sort the subgraphs. subgraphs with '->' are sorted first
    subgraphs.sort(function(a, b) {
      if (a.rawText.includes('->') && !b.rawText.includes('->')) {
        return -1;
      }
      if (!a.rawText.includes('->') && b.rawText.includes('->')) {
        return 1;
      }
      return 0;
    });

    var listOfElements = [];
    var alreadyAdded = {};
    subgraphs.forEach(function(subgraph) {
      var text = subgraph.rawText;

      if (subgraph.rawText.includes(' -> ')) {
        var node1 = subgraph.rawText.split(' -> ')[0];
        var node2 = subgraph.rawText.split(' -> ')[1].split(' ')[0];

        if (subgraph.rawText.includes('para') || subgraph.rawText.includes('def')) {
          var node = node1.includes('(') || node1.includes("<") ? node2 : node1;
          var otherNode  = node1.includes('(')  || node1.includes("<")  ? node1 : node2;

          // if the parameter is a method, most of the current checks will fail, so just hack around it
          if ((node1.includes('(') || node1.includes("<")) && (node2.includes('(') || node2.includes("<"))) {
            node      = node1.replace('.', '__');
            otherNode = node2.replace('.', '__');
          }
          
          if (!alreadyAdded[node]) {
            listOfElements.push({headerCount: headerCount, 
              header: {
                rawText: node,
                composite: true,
                otherNode: otherNode,
                edge: subgraph.edges[0],
                otherNode: otherNode,
                node: node,
                toolFeedback: null,
              },
              node: node,
            });

            headerCount += 1;
            alreadyAdded[node] = true;
          }   
        } else {
          if (!alreadyAdded[node1]) {

            listOfElements.push({headerCount: headerCount, header: {
              rawText: node1
            },
              node: node1,
              toolFeedback: null,
            });

            headerCount += 1;
            alreadyAdded[node1] = true;
          }

          if (!alreadyAdded[node2]) {
            listOfElements.push({headerCount: headerCount, header: {
                rawText: node2
              }, 
              node: node2,
              toolFeedback: null,
            });

            headerCount += 1;
            alreadyAdded[node2] = true;
          }
        }

      } else {

        if (!alreadyAdded[text]) {
          headerText[text] =  subgraph ;
      
          var hasFeedback = hastoolFeedback[text];

          listOfElements.push({headerCount: headerCount, header: headerText[text], 
            node: text,
            toolFeedback: hasFeedback,
          });

          headerCount += 1;
          alreadyAdded[text] = true;
        }
      }
    });

    // console.log(listOfElements);

    var elementRoles = identifyElementRoles();
    // console.log(elementRoles);

    var exampleClustersByRole = {};
    
    listOfElements
      .filter(function(element) {
        return elementRoles[element.node];
      })
      .forEach(function(element) {
        // console.log('[discriminativeSubgraphsSkeleton] going through listOfElements', element);
        var roles = elementRoles[element.node];
        roles.forEach(function(role) {

          if (role == 'guard' && exampleClustersByRole[role] && exampleClustersByRole[role].length >= 3 && !element.toolFeedback) {
            return;
          }

          if (exampleClustersByRole[role] && exampleClustersByRole[role].length >= 3 && !getFocalAPI().includes(element.node)&& !element.toolFeedback ) {
            return;
          }

          // for 'guard', do not add (order)
          if (role === 'guard' && element.header && element.header.edge && element.header.edge.label && element.header.edge.label.indexOf('(order)') !== -1) {
            return;
          }

          exampleClustersByRole[role] = exampleClustersByRole[role] || [];
          exampleClustersByRole[role].push(element);
        });
      });
    // console.log('[discriminativeSubgraphsSkeleton] exampleClustersByRole');
    // console.log(exampleClustersByRole);

    // go through method and delcaration, removing a node if they appear in otherNodes in parameter1
    if (exampleClustersByRole['parameter1'] && exampleClustersByRole['method']) {
      var parameter1Nodes = exampleClustersByRole['parameter1']
      .filter(function(subgraph) {
        return subgraph.header.otherNode;
      })
      .map(function(subgraph) {
        return escapeNodeForDisplay(subgraph.header.otherNode);
      });
      if (exampleClustersByRole['method']) {
        exampleClustersByRole['method'] = exampleClustersByRole['method'].filter(function(subgraph) {
          return !parameter1Nodes.includes(escapeNodeForDisplay(subgraph.node));
        });
      }
      if (exampleClustersByRole['declaration']) {
        exampleClustersByRole['declaration'] = exampleClustersByRole['declaration'].filter(function(subgraph) {
          return !parameter1Nodes.includes(escapeNodeForDisplay(subgraph.node));
        });
      }
      
    }

    // hide non-method looking stuff in 'method'
    if (exampleClustersByRole['method']) {
      exampleClustersByRole['method'] = exampleClustersByRole['method'].filter(function(subgraph) {
        return subgraph.header && subgraph.header.rawText && subgraph.header.rawText.includes('(');
      });
    }

    // if a method is a parameter
    // heuristically remove it from 'method'
    if (exampleClustersByRole['parameter1']) {
      var parameter1Nodes = exampleClustersByRole['parameter1']
      .map(function(subgraph) {
        return escapeNodeForDisplay(subgraph.node);
      });
      if (exampleClustersByRole['method']) {
        exampleClustersByRole['method'] = exampleClustersByRole['method'].filter(function(subgraph) {
          return !parameter1Nodes.includes(escapeNodeForDisplay(subgraph.node));
        });
      }
    }
    // if its an exception
    // insert a non-composite header in the catch (...) part
    if (exampleClustersByRole['exception']) {
      exampleClustersByRole['exception'].forEach(function(subgraph) {
        if (subgraph.header && subgraph.header.rawText) {
          subgraph.header.composite = false;
        }
      });

    }


    // as list
    var keyColors = {
      'declaration': '120',
      'guard': '160',
      'method': '200',
      'parameter1': '200',
      'exception': '260',
      'return': '220',
      'error': '280',
    }
    var roleList = [];

    exampleClustersByRole['guard'] = []; // quick hack to suppress ugly pattern
    for (var key of ['declaration', 'guard', 'parameter1', 'method', 'return', 'exception',  'error', undefined]) {
      var add = {
        role: key,
        cluster: structuredClone(exampleClustersByRole[key]),
        contextColor: keyColors[key],
      };
      roleList.push(add);

      // the examples will go under the cluster of the role
      if (add.cluster) {
        add.cluster.forEach(function(cluster, cluster_i) {
          cluster['role'] = key;  
          cluster['contextColor'] = keyColors[key] - cluster_i;
        });
      }
      
    }

    // console.log('[discriminativeSubgraphsSkeleton] returning', roleList, 'roleList')
    return roleList;
      
  },
  queryExamples(){
    
    // console.log('fetching query examples');

    // return Examples.find({'query': true});

    // temp
    // return Examples.find();

    // user study
    var examples = Examples.find({'label': {'$ne': '?'}}).fetch();
    return examples;

    // examples.sort(function(a, b) {
    //   if (a['prelabelled'] && !b['prelabelled']) {
        
    //     return -1;
    //   }
    //   if (!a['prelabelled'] && b['prelabelled']) {
    //     return 1;
    //   }

    //   if (a['prelabelled'] && b['prelabelled']) {
    //     // sort by their label, first show positive, then negative, then '?'
    //     if (a['label'] === 'positive' && b['label'] !== 'positive') {
    //       return -1;
    //     }
    //     if (a['label'] !== 'positive' && b['label'] === 'positive') {
    //       return 1;
    //     }
    //     if (a['label'] === 'negative' && b['label'] !== 'negative') {
    //       return -1;
    //     }
    //     if (a['label'] !== 'negative' && b['label'] === 'negative') {
    //       return 1;
    //     }
    //     return 0;
    //   } else {
    //     // sort by exampleID
    //     return a['exampleID'] - b['exampleID'];
    //   }
    // });

    // return examples;
  },
  baselineExamples() {

    buildSkeleton();
    var skeleton = Session.get('skeleton');

    var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));

    var examples = fetchShortestExamples(selector).fetch();

    // include unlabelled examples
    // examples = examples.concat(Examples.find({'label': '?'}).fetch());

    // examples.forEach(function(example) {
    //   example['clusterContext'] = 'baseline';
    // }
    // );
    // sort the examples.
    // first, show the prelabelled data
    // then sort by their label, first show positive, then negative, then '?'
    examples.sort(function(a, b) {
      if (a['prelabelled'] && !b['prelabelled']) {
        
        return -1;
      }
      if (!a['prelabelled'] && b['prelabelled']) {
        return 1;
      }

      if (a['prelabelled'] && b['prelabelled']) {
        // sort by their label, first show positive, then negative, then '?'
        if (a['label'] === 'positive' && b['label'] !== 'positive') {
          return -1;
        }
        if (a['label'] !== 'positive' && b['label'] === 'positive') {
          return 1;
        }
        if (a['label'] === 'negative' && b['label'] !== 'negative') {
          return -1;
        }
        if (a['label'] !== 'negative' && b['label'] === 'negative') {
          return 1;
        }
        return 0;
      } else {
        // sort by exampleID
        return a['exampleID'] - b['exampleID'];
      }
    });

    examples.forEach(function(example) {
      example['clusterContext'] = 'baseline';
      Meteor.defer(function() {
        render(example);
      });
    });

    return examples;

  },
  countLabelledExamples(){
    // find examples where label != '?'
    return Examples.find({'label': {'$ne': '?'}}).count()

  },
  countMatchingPositiveExamples() {
    
    buildSkeleton();

    var currentCount = computeMatchingPositiveExamples();
    if (Session.get('oldCountMatchingPositiveExamples')) {
      var oldCount = Session.get('oldCountMatchingPositiveExamples');
      var diff = currentCount - oldCount;
      if (diff > 0) {
        return currentCount + ' (+' + diff + ')';
      } else if (diff < 0) {
        return currentCount + ' (' + diff + ')';
      } else {
        return currentCount;
      }
    } else {
      return currentCount;
    }
  },
  countMatchingNegativeExamples() {

    buildSkeleton();
    var currentCount = computeMatchingNegativeExamples();
    if (Session.get('oldCountMatchingNegativeExamples')) {
      var oldCount = Session.get('oldCountMatchingNegativeExamples');
      var diff = currentCount - oldCount;
      if (diff > 0) {
        return currentCount + ' (+' + diff + ')';
      } else if (diff < 0) {
        return currentCount + ' (' + diff + ')';
      } else {
        return currentCount;
      }
    } else {
      return currentCount;
    }
  },
  countMatchingUnlabelledExamples() {
    
    buildSkeleton();
    var currentCount = computeMatchingUnlabelledExamples();

    if (Session.get('oldCountMatchingUnlabelledExamples')) {
      var oldCount = Session.get('oldCountMatchingUnlabelledExamples');
      var diff = currentCount - oldCount;
      if (diff > 0) {
        return currentCount + ' (+' + diff + ')';
      } else if (diff < 0) {
        return currentCount + ' (' + diff + ')';
      } else {
        return currentCount;
      }
    } else {
      return currentCount;
    }
  },
  countUnmatchingPositiveExamples() {
    buildSkeleton();
   
    var currentCount = computeUnmatchingPositiveExamples();
    if (Session.get('oldCountUnmatchingPositiveExamples')) {
      var oldCount = Session.get('oldCountUnmatchingPositiveExamples');
      var diff = currentCount - oldCount;
      if (diff > 0) {
        return currentCount + ' (+' + diff + ')';
      } else if (diff < 0) {
        return currentCount + ' (' + diff + ')';
      } else {
        return currentCount;
      }
    } else {
      return currentCount;
    }
  },
  countUnmatchingNegativeExamples() {
    buildSkeleton();
    
    var currentCount = computeUnmatchingNegativeExamples();
    if (Session.get('oldCountUnmatchingNegativeExamples')) {
      var oldCount = Session.get('oldCountUnmatchingNegativeExamples');
      var diff = currentCount - oldCount;
      if (diff > 0) {
        return currentCount + ' (+' + diff + ')';
      } else if (diff < 0) {
        return currentCount + ' (' + diff + ')';
      } else {
        return currentCount;
      }
    } else {
      return currentCount;
    }
  },
  countUnmatchingUnlabelledExamples() {
    buildSkeleton();
    var currentCount = computeUnmatchingUnlabelledExamples();
    if (Session.get('oldCountUnmatchingUnlabelledExamples')) {
      var oldCount = Session.get('oldCountUnmatchingUnlabelledExamples');
      var diff = currentCount - oldCount;
      if (diff > 0) {
        return currentCount + ' (+' + diff + ')';
      } else if (diff < 0) {
        return currentCount + ' (' + diff + ')';
      } else {
        return currentCount;
      }
    } else {
      return currentCount;
    }
  },
  primaryButtonIfZeroCountMatchingNegativeExamples() {
    var currentCount = computeMatchingNegativeExamples();
    if (currentCount === 0) {
      return 'btn-primary';
    } else {
      return '';
    }
  },
  openaiExamples(){
    return Queries.find({});

  },
  baggedExamples(){
    var selector = constructSelectorToMatchBaggedPatterns({ });
    var examples = fetchShortestExamples(selector);

    // examples.forEach(function(example) {
    //   render(example);
    // });
    console.log('[baggedExamples] returning', examples.count(), 'examples')

    examples = examples.fetch();
    // add clusterContext
    examples.forEach(function(example) {
      example['clusterContext'] = 'bagged';
    });

    return examples;
  },
  matchingExamples() {
    buildSkeleton();
    var skeleton = Session.get('skeleton');

    var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));

    var examples = fetchShortestExamples(selector).fetch();

    examples.forEach(function(example) {
      example.clusterContext = 'matching';
      Meteor.defer(function() {
        render(example);
      });
    });
    return examples;
  },
  matchingCount() {
    buildSkeleton();
    var skeleton = Session.get('skeleton');

    var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
    
    // console.log('fetchShortestExamples(selector).count()', fetchShortestExamples(selector).count());
    return fetchShortestExamples(selector).count();
  },
  
  isActive(mode) {
    return Session.get('view') === mode ? 'active' : '';
  },
  isActiveOrHidden(mode) {
    var isActive = false;
    if (Session.get('view') === mode) {
      isActive = true;
      return 'active'; // early return
    }
    
    var skeleton = Session.get('skeleton');

    // if there is nothing in the skeleton, then the mode is disabled
    if (_.isEmpty(skeleton)) return 'invisible';
    
    var hasChecked = false;
    Object.keys(skeleton).forEach(function(key) {
      if (skeleton[key].checked) {
        hasChecked = true;
      }
    }
    );
    if (!hasChecked) return 'invisible';
  },
  allOrMatchingText() {
    return hasSelectedPatternInPalette()? 'Matching' : 'All';
  },
  isPaletteVisible(mode) {
    if (Session.get('view') === mode) {
      return 'active'; // early return
    }
    
    var skeleton = Session.get('skeleton');

    // if there is nothing in the skeleton, then the mode is disabled
    if (_.isEmpty(skeleton)) return 'invisible';
    
    var hasChecked = false;
    Object.keys(skeleton).forEach(function(key) {
      if (skeleton[key].checked) {
        hasChecked = true;
      }
    }
    );
    if (!hasChecked) return 'invisible';
  },
  isActiveOrHiddenByLabel(mode) {
    // if no example is labelled yet, hide the buttons
    if (Examples.find({label: {$in: ['positive', 'negative']}}).count() === 0) return 'invisible';
    if (mode === '') return '';
    
    return Session.get('view') === mode ? 'active' : '';
  },
  currentKeyword() {
    return Session.get('keyword');
  },
  subgraphsMined() {
    var subgraphs = Subgraphs.find().fetch();
    return subgraphs;
  },
  topologicalSortNodesOfMinedDiscriminativeSubgraphs() {

    var subgraphs = Subgraphs.find({ '$and' : 
      [
        {'discriminative': true},  
        {'hidden': {'$ne': true}}
      ]
    }).fetch();

    
    // var sortedNodes = sortNodes(subgraphs);
    var sortedNodes = sortNodesFromEdges(subgraphs);


    
    return sortedNodes;
  },
  topologicalSortNodesOfAlternativeDiscriminativeSubgraphs() {
    var subgraphs = Subgraphs.find({'alternative': true}).fetch();


    console.log('alternative subgraphs', subgraphs);

    var sortedNodes = sortNodes(subgraphs);

    console.log('sortedNodes [topologicalSortNodesOfAlternativeDiscriminativeSubgraphs]', sortedNodes);
    return sortedNodes;

  },
  topologicalSortMinedSuggestedPatterns() {

    // HJ disabled:
    // return [];

    var viewType = Session.get('view');

    var subgraphs = subgraphsToDisplay(viewType);

    var sortedNodes = sortNodes(subgraphs);

    console.log('sortedNodes [topologicalSortMinedSuggestedPatterns]', sortedNodes);

    var skeleton = Session.get('skeleton');
    var examples = filterByView(skeleton).fetch();
    // examples.forEach(function(example) {
    //   render(example);
    // });
    
    return sortedNodes;
  },
  topologicalSortMinedDiscriminativeAndAlternativePatterns() {
    var subgraphs = Subgraphs.find({ '$and': [{ 'alternative': true },] }).fetch();

    console.log('suggested subgraphs by finding alternative patterns', subgraphs);
    // filter out "MISSING"
    subgraphs = subgraphs.filter(subgraph => subgraph.rawText !== "MISSING")
      .filter(subgraph => filterOnlyLiteralsAndCalls(subgraph.rawText));

    subgraphs.concat(Subgraphs.find(
      { '$and' : [{'discriminative': true},  ]}).fetch());

    var sortedNodes = sortNodesFromEdges(subgraphs);

    console.log('sortedNodes [topologicalSortMinedDiscriminativeAndAlternativePatterns]', sortedNodes);
    
    return sortedNodes;
  },
  countTooLowThreshold(){
    return Session.get('countTooLowThreshold');
  },
  selectorKeys(){
    var selector = Session.get('selector');
    // console.log('selectorKeys',selector);
    if (_.isEmpty(selector)){ 
      return [];
    } else{
      return Object.keys(selector);
    }
  },
  count() {
    buildSkeleton();
    var skeleton = Session.get('skeleton');
    // var selector = computeSelectorFromSkeleton(skeleton);
    // var selector = constructSelectorForExamplePane({});
    // buildSkeleton();
    // var skeleton = Session.get('skeleton');
    // var selector = computeSelectorFromSkeleton(skeleton);
    // console.log('selector:')
    // console.log(selector);

    var result = filterByView(skeleton).fetch().length
    return result;
    // return fetchAndCountExamples(selector);
  },
  

});

/*
 * Register the helper functions for the breadcrumb template
 */
Template.breadcrumb.helpers({
  shortName(filterType){
    var selector = Session.get('selector');
    var filterValue = selector[filterType];
    if (!_.isEmpty(filterValue)) {
      if (typeof filterValue === 'string' && filterValue !== 'dataset') {
        console.log('filterValue is string');
        return 'must have '+filterValue;
      } else if (Object.keys(filterValue)[0] === '$ne'){
        switch(filterType) {
          case "initialization":
            return "some declarations";
            break;
          case "try":
            return 'a try block';
            break;
          case "configuration":
            return "some config";
            break;
          case 'guardType':
            return "some guard type"
            break;
          case 'guardCondition':
            return 'some guard condition';
            break;
          // case 'focalAPI':
          //   return "the API call of interest";
          //   break;
          case 'checkType':
            return 'a control structure for results';
            break;
          case 'followUpCheck':
            return 'some result checking';
            break;
          case 'use':
            return 'some use calls';
            break;
          case 'exceptionType':
            return 'some exception caught';
            break;
          case 'exceptionHandlingCall':
            return 'some exception handling';
            break;
          // case 'finally':
          //   return 'finally block';
          //   break;
          case 'cleanUpCall':
            return 'some clean up';
            break;
          default:
            return '';
        }
      } else if (Object.keys(filterValue)[0] === '$all'){
        switch(filterType) {
          case "initialization":
            return "all these declaration(s): " + filterValue['$all'].join();
            break;
          // case "try":
          //   return 'API call in a try block';
          //   break;
          case "configuration":
            return "all this config: " + filterValue['$all'].join();
            break;
          // case 'guardType':
          //   return "control structure enclosing the API call" + filterValue['$all'].join();
          //   break;
          // case 'guardCondition':
          //   return 'at least one condition guarding execution of the API call' + filterValue['$all'].join();
          //   break;
          // case 'focalAPI':
          //   return "the API call of interest";
          //   break;
          // case 'checkType':
          //   return 'control structure interacting with the API call results';
          //   break;
          // case 'followUpCheck':
          //   return 'at least one condition checking the results';
          //   break;
          case 'use':
            return 'all these uses: ' + filterValue['$all'].join();
            break;
          case 'exceptionType':
            return 'all these exception(s) caught: ' + filterValue['$all'].join();
            break;
          case 'exceptionHandlingCall':
            return 'all these call(s) handling exceptions: '+ filterValue['$all'].join();
            break;
          // case 'finally':
          //   return 'finally block';
          //   break;
          case 'cleanUpCall':
            return 'all these clean-up calls: '+ filterValue['$all'].join();
            break;
          default:
            return '';
        }
      }
    }
  },
});


/*
 * Register the event listeners on the body template
 */
Template.body.events({
  'mouseenter .example-cluster' (event, instance) {
    var role = $(event.target).attr('data-cluster');
    $('.example-cluster[data-cluster="'+role+'"]').addClass('has_border');

  },
  'mouseleave .example-cluster' (event, instance) {
    var role = $(event.target).attr('data-cluster');
    $('.example-cluster[data-cluster="'+role+'"]').removeClass('has_border');
  },
  'click .change-dataset'(event, instance){
    console.log('API Change')
    // Session.set('dataset', 'get');
  },
  'click .toggle-collapse'(event,instance){
    // console.log('.toggle-collapse clicked',event.target)
    var hideOptions = Session.get('hideOptions');
    $('.collapse').show();
    if (hideOptions){
      $('.collapse').collapse('show');
      Session.set('hideOptions',false);
    } else {
      $('.collapse').collapse('hide');
      Session.set('hideOptions',true);
    }
  },
  'click .end-task-btn'(event, instance){
    Session.set('taskComplete', true);

    $('#taskCompleteModal').modal('show');;

    endTask();


  },
  'click .reset-pattern-btn' (event, instance) {
    var subgraphs = Subgraphs.find({ '$and' : 
      [
        {'discriminative': true},  
        {'hidden': {'$ne': true}},
        {'$or': [{ 'bags': { '$exists': false } }, { 'bags': { $eq: null } }]}
      ]
    }).fetch();

    console.log('subgraphIds (the server recomputes this data though)' + subgraphs.map((subgraph) => subgraph._id));    
    deleteDiscriminativeSubgraphs();
    // delete the subgraphs
    
  },
  'click .reset-labels-btn' (event, instance) {
    // reset the labels on all examples
    resetLabels();
    console.log('reset labels');

  },
  'click .reset-all-btn' (event, instance) {

    deleteDiscriminativeSubgraphs();
    resetLabels();
    console.log('reset all!');
  },

  'click .reset-state-btn' (event, instance) {
    resetState(); // reset 

    showStatus('Restarting task... When the right panel has cleared, click on "Bootstrap Pattern" to start again');
    showLoadingText();
  },
  'click .labelRadio'(event, instance) {
    var label = $(event.target).data('option');
    var exampleId = $(event.target).prop('name').split('--')[1];
    var methodName = $(event.target).data('method-name');

    var keyword = Session.get('keyword');
    updateLabels(exampleId, methodName, label, keyword);

    
  },
  'click .openAILabelRadio'(event, instance) {


    var label = $(event.target).data('option');
    var openAIExampleId = $(event.target).prop('name').split('--')[1];
    
    var isPositive = label == 'positive';
    var newProgram = Queries.findOne({_id: openAIExampleId}).rawCode;
    if (isPositive) {
      createNewExample(newProgram, 'positive');
    } else {
      createNewExample(newProgram, 'negative');
    }

  },

  'click .subgraphCheckbox'(event, instance) {

    var selector = Session.get('selector');
    if (selector == null){
      selector = {};
    }

    var skeleton = Session.get('skeleton');
    if (skeleton == null) {
      skeleton = {};
    }

    var nodeLabel = $(event.target).prop('name').split('--')[1];
    var subgraphId = $(event.target).data('subgraphs');
    var text = $(event.target).val();
    var escaped = text.replaceAll('.', '__');
    var checked = $(event.target).prop('checked');

    // // check the other nodes in the subgraph
    // $('.subgraph-'+subgraphId).each(function(){
    //   $(this).prop('checked', checked);
    // });
    if (hasSelectedPatternInPalette()) {
      Session.set('view', 'matching');
    }
    

    updateSubgraphs(subgraphId, escaped, checked, null, null, function() {

      var subgraphId = $(event.target).data('subgraphs');

      // find all nodes with the same subgraph id
      var subgraphNodes = $('.subgraph-'+subgraphId +'.visible');
      var subgraphNodeLabels = subgraphNodes.map(function(){
        return $(this).prop('name').split('--')[1];
      })
      .filter(function(key, nodeLabel){
        return nodeLabel.includes('<') ||  nodeLabel.includes('(') || nodeLabel.includes(':');
      })
      .get();
  
      Session.set('selector',selector);
  
      // Session.set('skeleton', {});
      buildSkeleton();
  
      Examples.find({}).forEach(function(example){
        render(example);
      });


      // validation
      // if the skeleton matches at least one labelled negative example, show a warning
      // first, construct the selector
      

      if (Session.get('view') == 'all') {
        Session.set('view', 'matching');
      }
    });
  },

  'click .open-label-query-examples-modal' (event, instance) {
    // computeQueryExamples();
    updateLabels(-1, '', '', '');
  },

  'click .open-openai-examples-modal' (event, instance) {
    getOpenaiCompletion(function() {});

  },
  'click .infer-pattern-btn' (event, instance) {      
    inferPatterns();

    $('#labelQueryExamplesModal').modal('hide');

    var isBaseline = Session.get('isBaseline');
    
    if (!isBaseline) {
      showStatus('Provide feedback by checking the "Suggest" checkboxes. A checkmark means the feature should be considered when "Reinfer Pattern" is clicked');
    }else{ 
      showStatus('Click on "View and label matching examples" to label more examples.');
    }
    hideLoadingText();
  },
  'click .openai-infer-pattern-btn' (event, instance) {
    inferPatterns();

    $("#openaiExamplesModal").modal('hide');
    
  },
  'click .view-baseline-matching-examples' (event, instance) {
    // var subgraphs = Subgraphs.find({ '$and' : 
    //   [
    //     {'discriminative': true},  
    //     {'hidden': {'$ne': true}},
    //     {'$or': [{ 'bags': { '$exists': false } }, { 'bags': { $eq: null } }]}
    //   ]
    // }).fetch();
    // subgraphs.forEach(function(subgraph){
    //   removeDiscriminative(subgraph._id, '', function() {});
    // });
  },
  'click .baseline-infer-pattern-btn' (event, instance) {
    
    inferPatterns();

    $('#labelAllExamplesModal').modal('hide');
    // showStatus('Inferring a pattern... Once the pattern appears, you can end the task by clicking on "End Task".');
    Session.set('taskComplete', true);

    // $('#taskCompleteModal').modal('show');;

    // endTask();
  },
  'click .refine-pattern-btn' (event, instance) {

    // simulateProgress();
    showStatus('Refining pattern...');
    showLoadingText();
    
    // fetch subgraphs with 'hints`
    var hintSubgraphs = Subgraphs.find({hint: true}).fetch();

    updateNodeFeedback(hintSubgraphs, function() {
      // clear hints
      clearHints();
      showStatus('Done refining pattern.');
      hideLoadingText();
      buildSkeleton();
    });

    // pass to the server


    // var ignoreSubgraphs = window.ignoreSubgraphs;
    // var subgraphs = hintSubgraphs.filter(function(subgraph){
    //   var hasIgnoredSubgraph = ignoreSubgraphs.split(',')
    //   .filter(function(ignoreSubgraph){
    //     return ignoreSubgraph != '';
    //   })
    //   .filter(function(ignoreSubgraph){
    //     if (subgraph.rawText.includes(ignoreSubgraph)) {
    //       return true;
    //     }
    //     return false;
    //   }
    //   );
    //   return hasIgnoredSubgraph.length == 0;
    // });

    // // TODO: clear hints
    
    // clearHints();
    
    // if (subgraphs.length == 0) {
    //   subgraphs = Subgraphs.find({hint: true}).fetch();
    // }

    // subgraphs.forEach(function(subgraph){
    //   var subgraphId = subgraph._id;
    //   var text = subgraph.rawText.replaceAll('.', '__');;
    //   updateSubgraphs(subgraphId, text, true, true, null, function() {
    //     // update the selector
    //     buildSkeleton();
    //     Session.set('selector', Session.get('skeleton'));
    //     Session.set('view', 'matching');

    //     showStatus('Done refining pattern. Once you are done refining the pattern and answering the dataset comprehension questions, click on the "End Task" button at the bottom of this pane.');
    //     hideLoadingText();
      
    //   });

      
    // });

  
    window.ignoreSubgraphs = '';


    var matchingPositiveExamples = computeMatchingPositiveExamples();
    var matchingNegativeExamples = computeMatchingNegativeExamples();
    var matchingUnlabelledExamples = computeMatchingUnlabelledExamples();
    var unmatchingPositiveExamples = computeUnmatchingPositiveExamples();
    var unmatchingNegativeExamples = computeUnmatchingNegativeExamples();
    var unmatchingUnlabelledExamples = computeUnmatchingUnlabelledExamples();
    // update old session variables
    Session.set('oldCountMatchingPositiveExamples', matchingPositiveExamples);
    Session.set('oldCountMatchingNegativeExamples', matchingNegativeExamples);
    Session.set('oldCountMatchingUnlabelledExamples', matchingUnlabelledExamples);
    Session.set('oldCountUnmatchingPositiveExamples', unmatchingPositiveExamples);
    Session.set('oldCountUnmatchingNegativeExamples', unmatchingNegativeExamples);
    Session.set('oldCountUnmatchingUnlabelledExamples', unmatchingUnlabelledExamples);

  },
  'click .showWhatIf' (event, instance) {
    
    // find the element with .showWhatIf
    var whatIf = $(event.target).closest('.showWhatIf');

    // console.log( whatIf.attr('data-whatif-subgraph-a') );
    // console.log( whatIf.attr('data-whatif-subgraph-b') );

    Session.set('whatIfSubgraphA', whatIf.attr('data-whatif-subgraph-a'));
    Session.set('whatIfSubgraphB', whatIf.attr('data-whatif-subgraph-b'));

    Session.set('whatIfSubgraphAColor', whatIf.attr('data-whatif-subgraph-a-color'));
    Session.set('whatIfSubgraphBColor', whatIf.attr('data-whatif-subgraph-b-color'));
  },
  'click .frequent-node' (event, instance) {
    var targetCollapse = $(event.target).data('target'); 
    $(targetCollapse).addClass('show');

    if ($(event.target).hasClass('expanded')) {
      $(event.target).addClass('unexpanded');
      $(event.target).removeClass('expanded');
    } else {
      $(event.target).addClass('expanded');
      $(event.target).removeClass('unexpanded');
    }

  },
    'change .frequentSubgraphCheckbox'(event, instance) {
    
      var skeleton = Session.get('skeleton', skeleton);
      if (skeleton == null) {
        skeleton = {};
      }


      var nodeLabel = $(event.target).prop('name').split('--')[1];

      var value = $(event.target).val();
      var isPattern = value === 'pattern';
      var isAntiPattern = value === 'anti-pattern';
      // var escaped = nodeLabel.replaceAll('.', '__');
      
      // var subgraphId = $(event.target).data('subgraph-id');
      // var subgraphId = $(event.target).attr('data-subgraph-id');



      // find the parents of the node
      var parents = $(event.target).data('parents');

      
      createNewSubgraph(parents + ',' + nodeLabel, true, isPattern && !isAntiPattern, function() {
      
      // updateSubgraphs(subgraphId, escaped, true, isPattern && !isAntiPattern, null, function() {
        // colorSubgraphNodeSelection([ subgraphId ]);

        buildSkeleton();
        fetchShortestExamples({}).forEach(function(example){
          render(example);
        });

        $('.frequentSubgraphCheckbox').val("");

        var skeleton = Session.get('skeleton');
        if (hasSelectedPatternInPalette()) {
          // change view
          if (Session.get('view') == 'all') 
            Session.set('view', 'matching');
        }

        
        
      });
      $('.frequentSubgraphCheckbox').val("");


    },
    'change .subgraphClusterHintCheckbox' (event, instance) {

      showStatus("Checking optimality of pattern components... This can take a while.");
      showLoadingText();

      var nodeLabel = $(event.target).prop('name').split('--')[1];
      // var value = $(event.target).val();

      var isChecked = $(event.target).prop('checked');
      // var isAntiPattern = value === 'anti-pattern';

      var edgeInfo = $(event.target).attr('data-edge').replaceAll('.', '__');


      addHint(edgeInfo, isChecked, function() {
        showStatus('Once you have provided your suggestions, you can update the pattern by clicking on "Refine Pattern"');
        hideLoadingText();
      });
 
    },

    'click .add-to-bag'(event, instance) {

      // fetch current displayed discrimiantive subgraphs
      var subgraphs = Subgraphs.find({ '$and' : 
      [
        {'discriminative': true},  
        {'hidden': {'$ne': true}}
      ]
    }).fetch();

      var subgraphIds = subgraphs.map(function(subgraph) {
        return subgraph._id;
      });

      // var nodeLabel = $(event.target).prop('name').split('--')[1];
      // var escaped = nodeLabel.replaceAll('.', '__');

      var numBags = Bags.find({}).count();
      
      html2canvas(document.querySelector("#pattern-palette")).then(canvas => {

        var image = canvas.toDataURL("image/png");
        // canvas.id = "screenshot-" + numBags;
        // canvas.style.display = "none";
        // document.body.appendChild(canvas)
      

        // fetch color
        var color = $('.add-to-bag-color').val();

        subgraphIds.forEach(function(subgraphId, i) {

          var subgraph = Subgraphs.findOne({_id: subgraphId});
          if (subgraph == null) {
            return;
          }

          addSubgraphToBag(subgraph._id, numBags, color, image, function() {

            buildSkeleton();
            fetchShortestExamples({}).forEach(function(example){
              render(example);
            });
            

            if (!hasSelectedPatternInPalette()) {
              // change view
              Session.set('view', 'all');
            }

          });
        });

      });

      $("#testPatternModal").modal("hide")

      // update view
      if (Session.get('view') === 'matching') {
        Session.set('view', 'all');
      }

      // updateLabels(-1, '', ''); 
    },
    'click .discriminative-cancel' (event, instance) {

        // set discriminative=false

        // var subgraphId = $(event.target).data('subgraph-id');
        var subgraphId = $(event.target).attr('data-subgraph-id'); // https://github.com/meteor/blaze/issues/155


        var subgraph = Subgraphs.findOne({_id: subgraphId});
        if (subgraph == null) {
          return;
        }

        var escaped = subgraph.rawText.replaceAll('.', '__');
        removeDiscriminative(subgraph._id, escaped, function() {
          buildSkeleton();
          // fetchShortestExamples({}).forEach(function(example){
          //   render(example);
          // });
          

          if (!hasSelectedPatternInPalette()) {
            // change view
            Session.set('view', 'all');
          }
  
        });
    },

    'click .add-new-example' (event, instance) {
      var newProgram = $('#new-example-content').val();
      
      // var isPositive = $('#new-example-label-positive').is(':checked');
      var isPositive = $('.new-example-label').val() == 'positive';
      // var isNegative = $('#new-example-label-negative').is(':checked');
      if (isPositive) {
        createNewExample(newProgram, 'positive');
      } else {
        createNewExample(newProgram, 'negative');
      }
      $('#newExampleModal').modal('hide');
    },
    'click .connect-repo-button' (event, instance) {
      var repoPath = $('#repo-path').val();
      // strip trailing spaces
      repoPath = repoPath.replace(/\s+$/, '');
      connectToRepo(repoPath);
    },

    'change #representativeSlider'  (event, instance) {
      console.log('change slider');
      var sliderValue = $(event.target).val();
      console.log(sliderValue);
      Session.set('representativenessWeight', sliderValue);
    },

    'change #diversitySlider'  (event, instance) {
      console.log('change slider');
      var sliderValue = $(event.target).val();
      console.log(sliderValue);
      Session.set('diversityWeight', sliderValue);
    },

    'change #informativenessSlider'  (event, instance) {
      console.log('change slider');
      var sliderValue = $(event.target).val();
      console.log(sliderValue);
      Session.set('informativenessWeight', sliderValue);
    },






});

/*
 * Declare the dictionary of global helper functions, where key is the function name and value is the function body
 */
var helpers = {
  conditionalCountBlockRow: function(blockname) {
    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return fetchAndCountExamples(selector); 
    } else {
      //if we're not already filtering for an element in this block
      //then filter for it
      if (!selector[blockname]) {
        //is it a checkbox-block or a radio-button block?
        if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
          selector[blockname] = {$ne: []};
        } else {
          selector[blockname] = {$ne: 'empty'};
        }
      }
      var conditionalCount = fetchAndCountExamples(selector);
      return conditionalCount;
    }
  },
  hideLabels: function(){
    // console.log(Session.get('hideLabels'));
    return Session.get('hideLabels');
  },
  blockStyle: function(blockname) {
    // return "inherit";
    return "";
  },
  countBlock: function(blockname) {
    var selector = {};
    if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
      selector[blockname] = {$ne: []};
    } else {
      selector[blockname] = {$ne: 'empty'};
    }
    return fetchAndCountExamples(selector);
  },
  count: function(optionname) {
    var selector = {};
    return fetchAndCountExamples(selector);
  },
  matchingCount: function() {
    buildSkeleton();
    var skeleton = Session.get('skeleton');

    var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
    
    // console.log('fetchShortestExamples(selector).count()', fetchShortestExamples(selector).count());
    return fetchShortestExamples(selector).count();
  },
  positiveCount: function(optionname) {
    // var parentData = Template.parentData();
    var selector = {'dataset': getDataset(), 'label' : 'positive' };
    
    return fetchAndCountExamples(selector);
  },
  negativeCount: function(optionname) {
    // var parentData = Template.parentData();
    var selector = {'dataset': getDataset(), 'label' : 'positive' };
    
    return fetchAndCountExamples(selector);
  },
  exampleType: function() {
    var viewType = Session.get('view');
    
    if (viewType === 'all') {
      return 'remaining';

    } else if (viewType === 'matching') {
      return 'matching';

    } else if (viewType == 'unlabelled') {
      return 'unlabelled';
    } else if (viewType == 'labelled') {
      return 'labelled';
    } else if (viewType === 'confused') {
      return 'mislabelled';
    } else if (viewType === 'not-matching') {
      return 'uncovered';
    }
  },
 
  matchingText: function() {
    var keyword = Session.get('keyword');
    var viewType = Session.get('view');
    
    if (keyword && viewType == 'matching') {
      return 'matching "' + keyword + '" and the Palette';
    }
    
    if (keyword) {
      return 'matching "' + keyword + '"';
    } 
    if (viewType == 'matching') {
      return 'matching the Palette';
    }
  },
  filteredBagCount: function() {
    var selector = Session.get('selector');

    var fullCountSelector = computeSelectorByViewAndKeyword(selector);
    var countSelector = computeSelectorByViewAndKeyword(selector);
    countSelector = constructSelectorToFilterBaggedPatterns(countSelector);
    
    return fetchAndCountExamples(fullCountSelector) - fetchAndCountExamples(countSelector);
  },
  discriminativeSubgraphcount: function() {
    return getAllSubgraphs(true).length;
  },

  frequentSubgraphcount : function() {
    return getAllSubgraphs(false).length;
  },

  conditionalTooltip: function(blockname){ 
    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return ""; 
    } else {
      if (!selector[blockname]){
        if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
          selector[blockname] = {$ne: []};
        } else {
          selector[blockname] = {$ne: 'empty'};
        }
      }
      var conditionalCount = fetchAndCountExamples(selector);
      var tooltip = ", " + conditionalCount + " of which have ";
      var count = 0;
      var length = Object.keys(selector).length;
      for (var key in selector) {
	 if (key !== 'dataset' && key !== blockname) {
	   tooltip += key;
	   if (count < length - 3) {
	     if (length === 4) tooltip += " and ";
	     else tooltip += ", ";
	   }
           count++;
	 }
      }

      return tooltip;
    }
  },
  conditionalTooltipOption: function(optionname){
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];

    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return "";
    } else {
      //is it a radio or check button?
      if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
        //it is a check box
        var block_selector_object = selector[blockname];
        if (_.isEmpty(block_selector_object) || _.isEqual(block_selector_object,{$ne: []}) ){
          var updated_list = [ optionname ];
        } else if (!_.isEmpty(block_selector_object['$all'])) {
          var list_of_options = block_selector_object['$all'];
          var updated_list = list_of_options.concat([ optionname ]);
        } else {
          console.log('unhandled case!!')
        }
        if (_.isEmpty(updated_list)){
          selector[blockname] = undefined;
        } else {
          selector[blockname] = { '$all': updated_list };
        }
      } else {
        if (!_.isEmpty(selector[blockname]) && !_.isEqual(selector[blockname],{$ne: 'empty'}) && selector[blockname] !== optionname ){
          //then another radio button is selected; dont override it
          return ""; 
        } else {
          selector[blockname] = optionname;
        }
      }
      var conditionalCount = fetchAndCountExamples(selector);
      var conditionalCount = fetchAndCountExamples(selector);
      var tooltip = ", " + conditionalCount + " of which have ";
      var count = 0;
      var length = Object.keys(selector).length;
      for (var key in selector) {
   	if (key !== 'dataset' && key !== blockname) {
     	  tooltip += key;
     	  if (count < length - 3) {
       	    if (length === 4) tooltip += " and ";
            else tooltip += ", ";
          }
          count++;
        }
      }

      return tooltip;
    }
  },

  labelledPositiveWidth: function(obj) {
    if (obj.subgraphIds == undefined) {
      return 0;
    }

    buildSkeleton();
    var skeleton = Session.get('skeleton');

    var subgraphId = obj.subgraphIds[0];

    var selector = computeSelectorFromSkeleton(skeleton);



    return computePositiveWidth(selector).toFixed(2);
  },
  labelledPositiveWidthCount: function(obj) {
    if (obj.subgraphIds == undefined) {
      return 0;
    }

    buildSkeleton();
    var skeleton = Session.get('skeleton');
    var selector = computeSelectorFromSkeleton(skeleton);
    if (Object.keys(selector) == 0) {
      return 0;
    }
  
    var examplesInViewSelector = computeSelectorByViewAndKeyword(skeleton);
    examplesInViewSelector = constructSelectorToFilterBaggedPatterns(examplesInViewSelector);
  
    if (examplesInViewSelector['$and']) {
      var mergedSelectorConjunction = selector['$and'].concat(examplesInViewSelector['$and']);
      var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
    } else {
      var mergedSelector = selector;
    }
  
    var conditionalPositiveMatches = fetchAndCountLabelledExamples(mergedSelector, 'positive');
    var conditionalPositiveCount = conditionalPositiveMatches.match;
    return conditionalPositiveCount
  },
  labelledNegativeWidth: function(obj) {
    if (obj.subgraphIds == undefined) {
      return 0;
    }

    buildSkeleton();
    var skeleton = Session.get('skeleton');

    var subgraphId = obj.subgraphIds[0];
    
    var selectorWithAdditionalSubgraph = computeSelectorWithAdditionalSubgraph(skeleton, subgraphId);
    // var selector = computeSelectorFromSkeleton(skeleton);

  
    
    return computeNegativeWidth(selectorWithAdditionalSubgraph).toFixed(2);
  },
  labelledNegativeWidthCount: function(obj) {
    if (obj.subgraphIds == undefined) {
      return 0;
    }

    buildSkeleton();
    var skeleton = Session.get('skeleton');
    var selector = computeSelectorFromSkeleton(skeleton);
    if (Object.keys(selector) == 0) {
      return 0;
    }
  
    var examplesInViewSelector = computeSelectorByViewAndKeyword(skeleton);
    examplesInViewSelector = constructSelectorToFilterBaggedPatterns(examplesInViewSelector);
  
    if (examplesInViewSelector['$and']) {
      var mergedSelectorConjunction = selector['$and'].concat(examplesInViewSelector['$and']);
      var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
    } else {
      var mergedSelector = selector;
    }
  
    var conditionalNegativeMatches = fetchAndCountLabelledExamples(mergedSelector, 'negative');
    var conditionalNegativeCount = conditionalNegativeMatches.match;
    return conditionalNegativeCount;
  },
  remainingLabelledPositiveWidth: function(obj) {
    if (obj.subgraphIds == undefined) {
      return 0;
    }

    buildSkeleton();
    var skeleton = Session.get('skeleton');
    if (skeleton == null) {
      skeleton = {};
    }
    var selector = computeSelectorFromSkeleton(skeleton);

    var subgraphId = obj.subgraphIds[0];

    // var subgraphs = [Subgraphs.findOne({subgraphId : subgraphId})];
    // var selector = computeSelector(subgraphs);
    // var conditionalCount = fetchAndCountExamples(selector);
    // console.log('returning ' + (100*conditionalCount/exampleTotal()).toFixed(2) +  ' for ' + obj.subgraphIds + ' with selector ' + JSON.stringify(selector));

    var selectorWithAdditionalSubgraph = computeSelectorWithAdditionalSubgraph(skeleton, subgraphId);

    var diff =  computePositiveWidth(selectorWithAdditionalSubgraph) - computePositiveWidth(selector);
    return diff.toFixed(2);
  },
  remainingLabelledNegativeWidth: function(obj) {
    if (obj.subgraphIds == undefined) {
      return 0;
    }

    buildSkeleton();
    var skeleton = Session.get('skeleton');
    if (skeleton == null) {
      skeleton = {};
    }
    var selector = computeSelectorFromSkeleton(skeleton);

    var subgraphId = obj.subgraphIds[0];

    // var subgraphs = [Subgraphs.findOne({subgraphId : subgraphId})];
    // var selector = computeSelector(subgraphs);
    // var conditionalCount = fetchAndCountExamples(selector);
    // console.log('returning ' + (100*conditionalCount/exampleTotal()).toFixed(2) +  ' for ' + obj.subgraphIds + ' with selector ' + JSON.stringify(selector));

    var selectorWithAdditionalSubgraph = computeSelectorWithAdditionalSubgraph(skeleton, subgraphId);

    var diff = computeNegativeWidth(selectorWithAdditionalSubgraph) - computeNegativeWidth(selector);
    return diff.toFixed(2);
  },

  unlabelledWidth: function(obj) {
    // buildSkeleton();
    if (obj.subgraphIds == undefined) {
      return 0;
    }

    var subgraphId = obj.subgraphIds[0];
    
    var subgraphs = [Subgraphs.findOne({_id : subgraphId})];
    if (subgraphs[0] == undefined) {
      return 0;
    }
    var singleSubgraphSelector = computeSelector(subgraphs);

    if (singleSubgraphSelector['$and'].length == 0) {
      return 0;
    }

    var skeleton = Session.get('skeleton');
    var examplesInViewSelector = computeSelectorByViewAndKeyword(skeleton);
    examplesInViewSelector = constructSelectorToFilterBaggedPatterns(examplesInViewSelector);

    if (examplesInViewSelector['$and']) {
      var mergedSelectorConjunction = singleSubgraphSelector['$and'].concat(examplesInViewSelector['$and']);
      var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
    } else {
      var mergedSelector = singleSubgraphSelector;
    }

    return (100 * fetchShortestExamples(mergedSelector).count() / fetchShortestExamples(examplesInViewSelector).count()).toFixed(2);
  },
  unlabelledWidthCount: function(obj) {
    buildSkeleton();
    if (obj.subgraphIds == undefined) {
      return 0;
    }

    var subgraphId = obj.subgraphIds[0];
    
    var subgraphs = [Subgraphs.findOne({_id : subgraphId})];
    if (subgraphs[0] == undefined) {
      return 0;
    }
    var singleSubgraphSelector = computeSelector(subgraphs);

    if (singleSubgraphSelector['$and'].length == 0) {
      return 0;
    }

    var skeleton = Session.get('skeleton');
    var examplesInViewSelector = computeSelectorByViewAndKeyword(skeleton);
    examplesInViewSelector = constructSelectorToFilterBaggedPatterns(examplesInViewSelector);

    if (examplesInViewSelector['$and']) {
      var mergedSelectorConjunction = singleSubgraphSelector['$and'].concat(examplesInViewSelector['$and']);
      var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
    } else {
      var mergedSelector = singleSubgraphSelector;
    }

    return fetchShortestExamples(mergedSelector).count();
  },
  unlabelledClusterWidth: function(obj) {
    var skeleton = Session.get('skeleton');
    var selector = computeSelectorFromSkeleton(skeleton);

    if (obj.header && obj.header.pseudo) {
      return 0;
    }
    var subgraphText;
    if (!obj.header.composite) {
      subgraphText = obj.header.rawText.replace(/\./g, '__') ;
    } else {
      subgraphText = obj.node;
    }

    var singleSubgraphSelector = {
      '$and': [{}]
    };
    singleSubgraphSelector['$and'][0][subgraphText] = {'$exists': true}
    selector = combineSelectorWithSingleSubgraphSelector(selector, singleSubgraphSelector);

    var skeleton = Session.get('skeleton');
    var examplesInViewSelector = computeSelectorByViewAndKeyword(skeleton);
    examplesInViewSelector = constructSelectorToFilterBaggedPatterns(examplesInViewSelector);


    var skeleton = Session.get('skeleton');
    var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));
    var matchingCount = fetchShortestExamples(selector).count();

    return (100 * obj.positiveExampleCount / matchingCount).toFixed(2);
  },
  unlabelledClusterWidthCount: function(obj) {
    return obj.positiveExampleCount;
  },
  remainingUnlabelledWidth: function(obj, subgraphId) {

    // buildSkeleton();

    // var subgraphs = [Subgraphs.findOne({subgraphId : subgraphId})];
    // var selector = computeSelector(subgraphs);
    // var conditionalCount = fetchAndCountExamples(selector);
    // // console.log('returning ' + (100*conditionalCount/exampleTotal()).toFixed(2) +  ' for ' + obj.subgraphIds + ' with selector ' + JSON.stringify(selector));
    // return (100*conditionalCount/exampleTotal()).toFixed(2);
    return 0;
  },
  conditionalWidth: function(blockname){ 
    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return 0; 
    } else {
      if (!selector[blockname]){
        if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
          selector[blockname] = {$ne: []};
        } else {
          selector[blockname] = {$ne: 'empty'};
        }
      }
      var conditionalCount = fetchAndCountExamples(selector);
      return (100*conditionalCount/exampleTotal()).toFixed(2);
    }

  },
  conditionalWidthOption: function(optionname){
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];

    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return 0;
    } else {
      //is it a radio or check button?
      if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
        //it is a check box
        var block_selector_object = selector[blockname];
        if (_.isEmpty(block_selector_object) || _.isEqual(block_selector_object,{$ne: []}) ){
          var updated_list = [ optionname ];
        } else if (!_.isEmpty(block_selector_object['$all'])) {
          var list_of_options = block_selector_object['$all'];
          var updated_list = list_of_options.concat([ optionname ]);
        } else {
          console.log('unhandled case!!')
        }
        if (_.isEmpty(updated_list)){
          selector[blockname] = undefined;
        } else {
          selector[blockname] = { '$all': updated_list };
        }
      } else {
        if (!_.isEmpty(selector[blockname]) && !_.isEqual(selector[blockname],{$ne: 'empty'}) && selector[blockname] !== optionname ){
          //then another radio button is selected; dont override it
          return 0 
        } else {
          selector[blockname] = optionname;
        }
      }
      var conditionalCount = fetchAndCountExamples(selector);
      return (100*conditionalCount/exampleTotal()).toFixed(2);
    }
  },
  remainingTotalWidth: function(blockname){
    var selector = {};

    if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
      selector[blockname] = {$ne: []};
    } else {
      selector[blockname] = {$ne: 'empty'};
    }

    var count = fetchAndCountExamples(selector);
    // console.log('remainingTotalWidth selector, count',selector,count);

    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return (100*count/exampleTotal()).toFixed(2);
    } else {
      if (!selector[blockname]){
        if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
          selector[blockname] = {$ne: []};
        } else {
          selector[blockname] = {$ne: 'empty'};
        }
      }
      // console.log('remainingTotalWidth selector, count',selector,count);
      var conditionalCount = fetchAndCountExamples(selector);
      // console.log('remainingTotalWidth count, conditionalCount',count,conditionalCount);
      return (100*(count - conditionalCount)/exampleTotal()).toFixed(2);
    }
  },
  remainingTotalWidthOption: function(optionname){
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];

    var selector = {};
    selector[blockname] = optionname;
    var count = fetchAndCountExamples(selector);

    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return (100*count/exampleTotal()).toFixed(2);
    } else {
      //is it a radio or check button?
      if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
        //it is a check box
        var block_selector_object = selector[blockname];
        if (_.isEmpty(block_selector_object) || _.isEqual(block_selector_object,{$ne: []})){
          var updated_list = [ optionname ];
        } else if (!_.isEmpty(block_selector_object['$all'])) {
          var list_of_options = block_selector_object['$all'];
          var updated_list = list_of_options.concat([ optionname ]);
        } else {
          console.log('unhandled case!');
        }
        if (_.isEmpty(updated_list)){
          selector[blockname] = undefined;
        } else {
          selector[blockname] = { '$all': updated_list };
        }
      } else {
        if (!_.isEmpty(selector[blockname]) && !_.isEqual(selector[blockname],{$ne: 'empty'}) && selector[blockname] !== optionname ){
          //then another radio button is selected; dont override it
          var conditionalCount = 0;
          return (100*(count - conditionalCount)/exampleTotal()).toFixed(2);
        } else {
          selector[blockname] = optionname;
        }
      }
      var conditionalCount = fetchAndCountExamples(selector);
      return (100*(count - conditionalCount)/exampleTotal()).toFixed(2);
    }
  },
  conditionalCountZeroBlock: function(blockname){
    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return ''; 
    } else {
      //if we're not already filtering for an element in this block
      //then filter for it
      if (!selector[blockname]) {
        //is it a checkbox-block or a radio-button block?
        if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
          selector[blockname] = {$ne: []};
        } else {
          selector[blockname] = {$ne: 'empty'};
        }
      }
      var conditionalCount = fetchAndCountExamples(selector);
      return conditionalCount;
    }
  },
  blockHeader: function(blockname) {
    switch(blockname) {
      case "initialization":
        return "declarations";
        break;
      case "try":
        return 'try block';
        break;
      case "configuration":
        return "configuration";
        break;
      case 'guardType':
        return "check type"
        break;
      case 'guardCondition':
        return 'check condition';
        break;
      case 'focalAPI':
        return "focus";
        break;
      case 'checkType':
        return 'follow-up check';
        break;
      case 'followUpCheck':
        return 'follow-up condition';
        break;
      case 'use':
        return 'uses';
        break;
      case 'exceptionType':
        return 'exceptions types';
        break;
      case 'exceptionHandlingCall':
        return 'exception handling';
        break;
      // case 'finally':
      //   return 'finally block';
      //   break;
      case 'cleanUpCall':
        return 'clean up';
        break;
      default:
        return '';
    }
  },
  recallOnTestSet: function(){

    var skeleton = Session.get('skeleton');
    var selector = constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton));

    var examples = fetchShortestExamples(selector).fetch();

    var targetIds  = []
    var targetExamples = []
    if (getDataset() == 'init') {
      // target ids = 1018, 1005, 1012, 1023, 1022
      targetIds = [ 1018, 1005, 1012, 1023, 1022 ];
      targetExamples = _.filter(examples, function(example){
        return _.contains(targetIds, example.exampleID);
      });


    } else if (getDataset() == 'random') {
      // target ids = 1021, 1011, 1015, 1002
      targetIds = [ 1021, 1011, 1015, 1002 ];
      targetExamples = _.filter(examples, function(example){
        return _.contains(targetIds, example.exampleID);
      });
    } else {
      // warm-up/demo. 
      //
      targetIds = [ 1024, 1002, 1022 ];
      targetExamples = _.filter(examples, function(example){
        return _.contains(targetIds, example.exampleID);
      });
    }

    var targetCount = targetIds.length;
    var targetCorrectCount = targetExamples.length;

    return targetCorrectCount / targetCount * 100;
    
    // return TestExamples.find(selector).count() / TestExamples.find().count() * 100;
  },
  toggleZeroCount: function(){
    return Session.get('toggleZeroCount');
  },
  conditionalAndTotalCountOption: function(optionname){
    //like conditionalCountOption but returns total when selector is empty
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return exampleTotal();
    } else {
      //is it a radio or check button?
      if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
        //it is a check box
        var block_selector_object = selector[blockname];
        if (_.isEmpty(block_selector_object) || _.isEqual(block_selector_object,{$ne: []})){
          var updated_list = [ optionname ];
        } else if (!_.isEmpty(block_selector_object['$all'])) {
          var list_of_options = block_selector_object['$all'];
          var updated_list = list_of_options.concat([ optionname ]);
        } else {
          console.log('unhandled case!')
        }
        selector[blockname] = { '$all': updated_list };
        return fetchAndCountExamples(selector);
      } else {
        if (!_.isEmpty(selector[blockname]) && !_.isEqual(selector[blockname],{$ne: 'empty'}) && selector[blockname] !== optionname ){
          //then another radio button is selected; dont override it
          return 0 
        } else {
          selector[blockname] = optionname;
          return fetchAndCountExamples(selector);
        }
      }
    }
  },
  conditionalCountZero: function(optionname){
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return '';
    } else {
      //is it a radio or check button?
      if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
        //it is a check box
        var block_selector_object = selector[blockname];
        //console.log('block_selector_object',block_selector_object);
        if (_.isEmpty(block_selector_object) || _.isEqual(block_selector_object,{$ne: []})){
          var updated_list = [ optionname ];
        } else if (!_.isEmpty(block_selector_object['$all'])) {
          var list_of_options = block_selector_object['$all'];
          var updated_list = list_of_options.concat([ optionname ]);
        } else {
          console.log('unhandled case!')
        }
        selector[blockname] = { '$all': updated_list };
        return fetchAndCountExamples(selector);
      } else {
        if (!_.isEmpty(selector[blockname]) && !_.isEqual(selector[blockname],{$ne: 'empty'}) && selector[blockname] !== optionname ){
          //then another radio button is selected; dont override it
          return '' 
        } else {
          selector[blockname] = optionname;
          return fetchAndCountExamples(selector);
        }
      }
    }
  },
  getBlockName: function() {
    var parentData = Template.parentData();
    return parentData['blockname'];
  },
  checkboxType: function(optionname){
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    return !_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}));
  },
  options: function(blockname) {
    // uncomment if you want to not replicate block and option level try
    // if (blockname === 'try'){
    //   return [];
    // }
    var selector = {};
    if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))) {
      selector[blockname] = {$ne: []};
      var examps = fetchExamples(selector);
      var calls = new Pycollections.Counter();
      _.each(examps, function(examp){
        calls.update(examp[blockname]);
      });
      var uniq_opts = calls.mostCommon().map(function(item){ return item[0]}); //unnecesary to call mostCommon here but fine
    } else {
      selector[blockname] = {$ne: 'empty'};
      // var opts = fetchExamples(selector);
      //consider rewritting this to use pycollections.js
      var uniq_opts = _.uniq(fetchExamples(selector).map(function (rec) {
            return rec[blockname];
        }), false);
    }
    var counts = {}
    _.each(uniq_opts, function(opt){ 
      var selector = {};
      selector[blockname] = opt;
      counts[opt] = fetchAndCountExamples(selector);
    });
    return _.sortBy(uniq_opts, function(opt){ return counts[opt]; }).reverse().slice(0, Session.get('numOptions'));
  },
  methodName: function(rawCode) {
    
    var firstline = _.first(
      _.filter(rawCode.split(' '), function(item){ return item.includes('(')  })
    )
    return firstline.split('(')[0];
  },

  isCorrectUseChecked: function(label) {
    return label == 'positive';
  },
  isMisuseChecked: function(label) {
    return label == 'negative';
  },
  checkedIfPositiveLabel: function(obj) {
    return obj.label == 'positive' ? 'checked' : '';
  },
  checkedIfNegativeLabel: function(obj) {
    return obj.label == 'negative' ? 'checked' : '';
  },
  showIfPositiveLabel: function(obj) {
    return obj.label == 'positive' ? '' : 'visibility:hidden';
  },
  showIfNegativeLabel: function(obj) {
    return obj.label == 'negative' ? '' : 'visibility:hidden';
  },
  showIfUnlabelled: function(obj) {
    return obj.label == '?' ? '' : 'visibility:hidden';
  },
  exampleColoring: function(obj) {
    // the following code was previously used for the unmatched examples in the impact analysis 
    // if (obj.showAsRemoved) {            
    //   return 'rgba(180,0,0, 0.6)';
    // } else {
      if (obj.label == 'positive') {
        return 'rgba(0,180,0, 0.6)';
      } else if (obj.label == 'negative') {
        return 'rgba(180,0,0, 0.6)';
      }
      return 'rgba(0,100,225, 0.6)'
    // }
  },
  baggedExampleHighlight: function(obj) {
    if (obj.label == 'positive') {
      return 'example-highlighted-correct';
    }
    if (obj.label == 'negative') {
      return 'example-highlighted-misuse';
    }
    return '';
  },
  debugNodes: function(obj) {
    return JSON.stringify(Object.keys(obj));
  },
  csv : function(debug, subgraphIds) {
    // console.log('csv',debug, subgraphIds);
    if (subgraphIds == undefined) return '';
    return subgraphIds.join(',');
  },
  showIfFocalNode: function(node) {
    if (escapeNodeForDisplay(node.text) == escapeNodeForDisplay(getFocalNode())) {
      return 'show in';
    }
    // alternatively, one of its descendents matches
    var children = node.children;
    if (children == undefined) return '';
    var descendents = []
    descendents.push.apply(descendents, children);
    while (descendents.length > 0) {
      var child = descendents.pop();
      if (escapeNodeForDisplay(child.text) == escapeNodeForDisplay(getFocalNode())) {
        return 'show in';
      }
      if (child.children != undefined) {
        descendents.push.apply(descendents, child.children);
      }
    }

    return '';
  },

  hideIfHaveNonEmptyChildren: function(node) {
    if (node.children == undefined && getFocalNode() == node.text ) return 'hide';
    if (node.children != undefined) {
      for (var i = 0; i < node.children.length; i++) {
        if (node.children[i].text != '') {
          return 'hide hadChildrenButEmpty';
        }
      }
    }
    return '';
  },
  hideIfBaseline: function(node) {
    if (Session.get('isBaseline')) {
      return 'hidden';
    }
  },
  hideIfNotBaseline: function(node) {
    if (!Session.get('isBaseline')) {
      return 'hidden';
    }
  },
  blurIfPreview  : function(obj) {
    return obj.isPreview ? 'opacity-50' : '';
  },

  joinDash : function(subgraphIds) {
    if (subgraphIds == undefined) return '';
    return subgraphIds.join('-');
  },

  allDiscriminativeSubgraphs : function() {
    return getAllSubgraphs(true);
  },

  allFrequentSubgraphs : function() {
    return getAllSubgraphs(false);
  },

  showIfContains : function(subgraphIds, subgraphId) {
    return subgraphIds.indexOf(subgraphId) != -1 ? 'visible' : 'invisible';
  },
  boundedBoxIfFocalAPI: function(node) {
    // console.log(node);
    if (node.text == getFocalAPI()) {
      return 'boundedBox';
    }
    return '';
  },
  boundedBoxIfPseudo: function(node) {
    // console.log(node);
    if (node.subgraphIds[0].includes('pseudo') && !node.subgraphIds[0].includes('focal')) {

      return 'boundedBox';
    }
    return '';
  },
  backgroundColorIfPseudo: function(node) {

    if (node.subgraphIds[0].includes('pseudo') && !node.subgraphIds[0].includes('focal')) {
      var contextColor = node['contextColor'];
      return 'background-color:hsla(' + contextColor + ', 60%, 60%, 0.9)';
    }
    return '';
  },
  invisibleIfPseudo: function(node) {
    // console.log(node);
    if (node.subgraphIds && node.subgraphIds[0].includes('pseudo') && !node.subgraphIds[0].includes('focal')) {

      return 'invisible';
    }
    if (node.header && node.header.pseudo) {
      return 'invisible';
    }
    return '';
  },
  hideIfPseudo: function(node) {
    // console.log(node);
    if (node.subgraphIds[0].includes('pseudo') && !node.subgraphIds[0].includes('focal')) {

      return 'hidden';
    }
    return '';
  },
  checkedIfHasFeedback: function(node) {
    return node.toolFeedback ? 'checked' : '';
  },
  showFeedback: function(node) {
    if (!node.toolFeedback) {
      return '';
    }
    return node.toolFeedback;
  },
  showIfHasFeedback: function(node) {

    if (node.toolFeedback) {
      return 'display: block';
    }
    return 'display: none;';
  },
  clusterMarginBottom: function(role) {
    if (role == 'method') {
      return '0'
    }
    if (role == 'error') {
      return '0';
    }
    return '8px';
  },
  clusterMarginLeft: function(role) {
   
    if (role == 'guard') {
      return '20px';
    }
    if (role == 'method') {
      return '40px';
    }
    if (role == 'return') {
      return '40px';
    }
    if (role == 'parameter1') {
      return '40px';
    }
    if (role == 'exception') {
      return '20px';
    }
    if (role == 'error') {
      return '40px';
    }
    
  },
  rolePrefix: function(role) {

    
    if (role.role == 'guard') {
      return '<div class="row row-alignment" style="padding:0"><div class="row row-alignment" style="padding:0; "><div class="col-md-1"></div><div class="col-md-3" style="width:50px"></div><div class="col-md-1"></div> <div class="col-md-1"></div> <div class="col-md-5 code-background" style="background-color:hsla(260, 60%, 60%, 0.7); color:black;"><span class="hljs-keyword">try</span> {</div> </div> </div>'
    }
    if (role.role == 'exception') {
      return '<div class="row row-alignment" style="padding:0"><div class="row row-alignment" style="padding:0;"><div class="col-md-1"></div><div class="col-md-3" style="width:50px"></div><div class="col-md-1"></div> <div class="col-md-1"></div> <div class="col-md-5 code-background" style="background-color:hsla(260, 60%, 60%, 0.7); color:black;">} <span class="hljs-keyword">catch</span> (</div></div>';
    }

    if (role.role == 'error') {
      return '<div class="row row-alignment" style="padding:0"><div class="row row-alignment" style="padding:0;"><div class="col-md-1"></div><div class="col-md-3" style="width:50px"></div><div class="col-md-1"></div> <div class="col-md-1"></div> <div class="col-md-5 code-background" style="background-color:hsla(280, 60%, 60%, 0.7); color:black">) {</div></div></div>';
    }

    if (role.role == undefined) {
      return '<div class="row row-alignment" style="padding:0"><div class="row row-alignment" style="padding:0; "><div class="col-md-1"></div><div class="col-md-3" style="width:50px"></div><div class="col-md-1"></div> <div class="col-md-1"></div> <div class="col-md-5 code-background" style="background-color:hsla(280, 60%, 60%, 0.7); color:black">}</div></div></div>';
    }
  },
  roleSuffix: function(role) {
    // if (role.role == 'parameter1') {
    //   return '<div class="row row-alignment" style="padding:0"><div class="row row-alignment" style="padding:0"><div class="col-md-1"></div><div class="col-md-3" style="width:50px"></div><div class="col-md-1"></div> <div class="col-md-7 code-background" style="padding-left:40px; background-color:hsla(280, 60%, 60%, 0.7); color:black"> } </div></div> </div>'
    // }
    return '';
  },
  discriminativeRolePrefix : function(role) {
    if (role.role == 'guard') {
      return '<div class="row row-alignment" style=""><div class="row-alignment" style="">  <div class="code-background" style="background-color:hsla(260, 60%, 60%, 0.7); color:black;"><span class="hljs-keyword">try</span> {</div> </div> </div>'
    }
    if (role.role == 'exception') {
      return '<div class="row row-alignment" style=""><div class="row-alignment" style="">  <div class="code-background" style="padding-left:20px; background-color:hsla(260, 60%, 60%, 0.7); color:black;"></div></div> </div><div class="row row-alignment" style=""><div class="row-alignment" style=""> <div class="code-background" style="background-color:hsla(260, 60%, 60%, 0.7); color:black">} <span class="hljs-keyword">catch</span> (</div></div>';
    }

    if (role.role == 'error') {
      return '<div class="row row-alignment" style=""><div class="row-alignment" style="">  <div class="code-background" style="background-color:hsla(270, 60%, 60%, 0.7); color:black;">) {</div></div></div>';
    }

    if (role.role == undefined) {
      return '<div class="row row-alignment" style=""><div class="row-alignment" style="">  <div class="code-background" style="background-color:hsla(280, 60%, 60%, 0.7); color:black;">}</div></div></div>';
    }
  },


  codeFormPrefix: function(role) {
    if (role == 'method') {
      return ' ';
    } 
    if (role == 'guard') {
      return '<span class="hljs-keyword"> if </span> (...';
    }
    if (role =='parameter1') {
      return '';
    }
  },
  codeFormSuffix: function(role) {
    if (role == 'method') {
      return ' ';
    } 
    if (role == 'guard') {
      return '...) {';
    }
    if (role =='parameter1') {
      return '';
    }

  },
  edgeInfo: function(node) {
    if (!node.composite)
      return node.rawText;
    
    return node.edge.rawText;

  },
  printParent: function(node) {

    if (node.parent == undefined) return '';

    // join by comma
    return node.parent.join(',');
  },

  correctOrMisuseClass: function(subgraphIds) {
    if (subgraphIds == undefined) return '';
    var one = Subgraphs.findOne({ _id: subgraphIds[0] });
    if (one == undefined) return '';
    return one.isPattern ? 'correct' : 'misuse';
  },

  hideCancelButton: function(obj) {

    
    var subgraphId = obj.subgraphIds[0];

    return Subgraphs.find({ _id: subgraphId, preview:true}).count() >= 1 ? 'hidden' : '';
  },
  isSubgraphChecked : function(node) {
    // var nodeText = node.text;
    return Subgraphs.find({ _id: node.subgraphIds[0], discriminative:true, labelled:true }).count() >= 1 ? 'checked' : '';
  },
  displayNode: function(text) {
    var result = escapeNodeForDisplay(text);

    return result;
  },

  roundOff: function(num) {
    if (num == undefined) return '';
    return num.toFixed(2);
  },
  displayRole: function(role) {
    if (role == undefined) return '';
    if (role == 'parameter1') return '// Pre-Method Call';
    if (role == 'method') return '';
    if (role == 'return') return '// Post-Method Call';
    if (role == 'error') return '// Error Handling';
    return '// ' + role[0].toUpperCase() + role.slice(1);
  },
  otherNode: function(node) {
    if (node.composite) {
      return escapeNodeForDisplay(node.otherNode);
    }
    return '';
  },
  thisNode: function(node) {
    if (node.composite) {
      return escapeNodeForDisplay(node.node);
    }
    return node;
  },
  displayHeader: function(node) {

    if (node.composite) {
      if (node.edge.label.includes('order')) {
        var precedingNode = node.edge.rawText.split(' -> ')[0];
        var followingNode = node.edge.rawText.split(' -> ')[1].split(' (order')[0];

        // var suffix = " ::: " + escapeNodeForDisplay(node.edge.rawText.split(' -> ')[1].split(' (order')[0]) + ' <small>... followed by ...</small> ' + escapeNodeForDisplay(node.edge.rawText.split('->')[0]);
        // var suffix = '(  ' + node.edge.rawText + ')' + " other: " + node.otherNode;
        // var suffix = "";
        if (precedingNode.replaceAll('.', '__') == node.otherNode) {
          return escapeNodeForDisplay(followingNode) 
        } else {
          return escapeNodeForDisplay(precedingNode) 
        }
        // return escapeNodeForDisplay(node.edge.rawText.split('->')[1].split(' (order')[0]) + ' <small>... followed by ...</small> ' + escapeNodeForDisplay(node.edge.rawText.split('->')[0]);
        // return escapeNodeForDisplay(node.edge.rawText.split('->')[1].split(' (order')[0]) 
      } else if (node.edge.label.includes('para')) {
        // special case: <catch>
        // just display the rawText of the non catch node
        if (node.edge.rawText.includes('catch')) {
          if (node.otherNode.includes("catch")) {
            return escapeNodeForDisplay(node.rawText);
          } else {
            return escapeNodeForDisplay(node.otherNode);
          }

          
        }

        if (node.otherNode.includes("(") && node.rawText.includes("(")) {  // both seem to be method calls!
          return escapeNodeForDisplay(node.otherNode).split('(')[0] + '(...' + escapeNodeForDisplay(node.rawText) + '...)';
        }

        if (node.otherNode.includes(":") || node.rawText.includes("(") || node.rawText.includes("<")) {
          return escapeNodeForDisplay(node.rawText).split('(')[0] + '(...' + escapeNodeForDisplay(node.otherNode) + '...)';
        } else {
          return escapeNodeForDisplay(node.otherNode).split('(')[0] + '(...' + escapeNodeForDisplay(node.rawText) + '...)';
        }
      } else if (node.edge.label.includes('def')) {
        if (node.otherNode.includes(":") || node.rawText.includes("(") || node.rawText.includes("<")) {
          return escapeNodeForDisplay(node.otherNode) + ' = ' + escapeNodeForDisplay(node.rawText) ;
        } else {
          return escapeNodeForDisplay(node.rawText) + ' = ' + escapeNodeForDisplay(node.otherNode) ;
        }
      } 
     
      // console.log('[displayHeader] node11: ' + node.rawText);
      return escapeNodeForDisplay(node.rawText);

    } else {
      // console.log('[displayHeader] node: ' + node.edge.rawText);
      if (node.pseudo) {
        return "----" + escapeNodeForDisplay(node.edge.rawText) + "----";
      }
      return escapeNodeForDisplay(node.rawText);
    }
  },
  countExamples: function(examples) {
    return examples.length;
  },
  examplesMatchedBySkeleton: function() {
    buildSkeleton();
    var skeleton = Session.get('skeleton');
    if (_.isEmpty(skeleton)){
      return 0;
    }
    // apply the skeleton selector
    var selector = computeSelectorFromSkeleton(skeleton);
    selector = constructSelectorToFilterBaggedPatterns(selector);
    // find all examples that match the selector
    return fetchAndCountExamples(selector);
  },
  baggedPatterns: function() {
    // return subgraphs where bag exists
    var subgraphsInBags = Subgraphs.find({ bags: { $exists: true, $ne: null } }).fetch();

    // find unique bags, using a flatmap on the subgraph.bags

    // var bags = _.uniq(subgraphsInBags.map(function(subgraph) {
    //   return subgraph.bags;
    // }
    // ));
    const uniqueBags = [...new Set(subgraphsInBags.flatMap(subgraph => subgraph.bags))];

    return Bags.find({ bagId: { $in: uniqueBags } }).fetch();

    // return uniqueBags;
  },
  disabledIfNoHints: function() {
    return Subgraphs.find({hint: true}).count() == 0? 'disabled' : '';

  },
  examplesMatched: function(bagId) {
    // first pick all bags chronologically earlier than this one
    var oldBags = Bags.find({ bagId: { $lt: bagId } }).fetch();

    // first match examples that match all the old bags
    var selector = {};
    var allMatchedExamples = [];
    _.each(oldBags, function(bag) {
      // pick subgraphs from this bag
      var subgraphs = Subgraphs.find({ bags: bag.bagId }).fetch();
      if (subgraphs.length == 0) return;

      var selector = computeSelector(subgraphs);
      var matchedExamples = fetchExamples(selector);
      allMatchedExamples.push(...matchedExamples);
    });
    
    var subgraphs = Subgraphs.find({ bags: bagId })
      .fetch();
    var selector = computeSelector(subgraphs);
      
    // selector['bags'] = bagId;

    var matchedExamples = fetchExamples(selector);
    var newlyMatchedExamples = _.difference(matchedExamples, allMatchedExamples);
    return newlyMatchedExamples.length;
  },
  
  misusesMatchedText : function(bagId) {
    var count = misusesMatched(bagId);
    return '';
    // return count > 0 ? 'but' + count + ' examples are misuses ' : '';
  },
  addToBagWarningMessage: function() {
    // if (Session.get('add-to-bag-warning-message') != undefined) return Session.get('add-to-bag-warning-message');

    var selector = {};
    buildSkeleton();
    var skeleton = Session.get('skeleton');
    var selector = computeSelectorFromSkeleton(skeleton);
    var conditionalNegativeMatches = fetchAndCountLabelledExamples(selector, 'negative');
    var conditionalNegativeCount = conditionalNegativeMatches.match;
  
    if (conditionalNegativeCount > 0) {
      // Session.set('add-to-bag-warning-message', 'The Palette Pattern matches ' + conditionalNegativeCount + ' examples marked as misuses');
    } else {
      Session.set('add-to-bag-warning-message', '');
    }
    return Session.get('add-to-bag-warning-message');
  },
  disableIfEmptyPalette: function() {
    return Subgraphs.find({ discriminative: true, labelled: true }).count() == 0 ? 'disabled' : '';
  },
  disabledIfNoDiscriminativeSubgraphs: function() {
    return Subgraphs.find({ discriminative: true, hidden:true }).count() == 0 ? 'disabled' : '';
  },
  showIfNoDiscriminativeSubgraphs: function() {
    return Subgraphs.find({ discriminative: true, hidden:true }).count() == 0 ? '' : 'display: none;';
  },
  bagPreviewClass: function(bagId) {
    var previewBag = Session.get('previewBag');

    if (previewBag == undefined) return 'bag-preview';

    if (previewBag == bagId) {
      return 'bag-preview-end active';

    } else {
      return 'bag-preview disabled';
    }
  },
  bagPreviewText: function(bagId) {
    var previewBag = Session.get('previewBag');

    if (previewBag == undefined) return 'Preview';

    if (previewBag == bagId) {
      return 'End Preview';

    } else {
      return 'Preview';
    }
  },
  bagWidth: function(bagId) {
    var subgraphs = Subgraphs.find({ bags: bagId })
      .fetch();
    var selector = computeSelector(subgraphs);
    var matchedExamples = fetchExamples(selector);

    // var fullSelector = constructSelectorForExamplePane({});
    var fullMatchedExamples = fetchShortestExamples({}).fetch();
    // var fullMatchedExamples = fetchExamples(fullSelector);

    // multiply by 2 ("* 2 " ) since we want to show two bars
    return (matchedExamples.length / 2 / fullMatchedExamples.length) * 100;
  },
  bagImage: function(bagId) {
    var bag = Bags.findOne({ bagId: bagId });
    return bag.image;

  }
  
  
};

/*
 * Register the global helper functions for all templates
 */
_.each(helpers, function(value, key){
  Template.registerHelper(key, value);
});



function sortExamples(examples) {
  examples.sort(function (a, b) {
    if (a['prelabelled'] && !b['prelabelled']) {

      return -1;
    }
    if (!a['prelabelled'] && b['prelabelled']) {
      return 1;
    }

    if (a['prelabelled'] && b['prelabelled']) {
      // sort by their label, first show positive, then negative, then '?'
      if (a['label'] === 'positive' && b['label'] !== 'positive') {
        return -1;
      }
      if (a['label'] !== 'positive' && b['label'] === 'positive') {
        return 1;
      }
      if (a['label'] === 'negative' && b['label'] !== 'negative') {
        return -1;
      }
      if (a['label'] !== 'negative' && b['label'] === 'negative') {
        return 1;
      }
      return 0;
    } else {
      // sort by exampleID
      return a['exampleID'] - b['exampleID'];
    }
  });
}

function determineChoicesOfCandidates(findCandidateNodes, computeSelectorAfterFeatureSelection) {
  var subgraphs = Subgraphs.find({ '$and' : 
    [
      {'discriminative': true},  
      {'hidden': {'$ne': true}},
      {'$or': [{ 'bags': { '$exists': false } }, { 'bags': { $eq: null } }]}
    ]
  }).fetch();
  if (subgraphs.length == 0) return [];

  buildSkeleton();
  var skeleton = Session.get('skeleton');
  
  var subgraphWithHints = {};
  Subgraphs.find({hint: true}).forEach(function(subgraph) {
    subgraphWithHints[subgraph.rawText] = true;
  });
  var subgraphs = findCandidateNodes(subgraphWithHints);
  // console.log('pattern growing candidates');
  // console.log(subgraphs);

  var headerText = {};
  var mapping = {};
  var negativeMapping = {};
  var mustMatchEdges = {};
  var hastoolFeedback = {};
  
  var subgraphEntropy = {};
  var subgraphUnmatchedEntropy = {};
  var subgraphLabelledPositiveMatches = {};
  var subgraphLabelledNegativeMatches = {};
  var skeletonEntropy = -1000 ;
  var totalExamples = Session.get('totalExamples') ? Session.get('totalExamples') : fetchShortestExamples().count();
  subgraphs.forEach(function(subgraph) {

    var text = subgraph.composite ? subgraph.edge.rawText : subgraph.rawText;


    if (mapping[text] || negativeMapping[text]) {
      return;
    }

    hastoolFeedback[text] = subgraph.toolFeedback;
    headerText[text] =  subgraph ;

    // console.log('subgraph', subgraph);

    var selectorWithAdditionalSubgraph = computeSelectorAfterFeatureSelection(skeleton, headerText[text].rawText);
    // console.log(selectorWithAdditionalSubgraph);

    if (subgraph.edge && subgraph.edge.from && subgraph.edge.to) {
      var edge = subgraph.edge;
      var edgeKey = edge.from + ' -> ' + edge.to + ' ' + edge.label ;
      // if (!mustMatchEdges[edgeKey]) {
      var newMatch = {};
      newMatch[edgeKey] = {
        $exists: true
      };

      selectorWithAdditionalSubgraph['$and'].push(newMatch);

    }

    var examplesAfter = fetchShortestExamples(constructSelectorToFilterBaggedPatterns(selectorWithAdditionalSubgraph));
    var examplesAfterIDs = examplesAfter.map(function(example) {
      return example._id;
    });

    examplesAfter.forEach(function(example) {
      mapping[text] = mapping[text] || [];
      
      // check if mapping[text] already has example with the same id
      var hasExample = false;
      mapping[text].forEach(function(existingExample) {
        if (existingExample._id === example._id) {
          hasExample = true;
        }
      });
      if (!hasExample) {
        mapping[text].push(example);
      }

    });

    
    // next, find examples that are not matched by the subgraphs

    var examplesBefore = fetchShortestExamples(constructSelectorToFilterBaggedPatterns(computeSelectorFromSkeleton(skeleton)));
    var examplesBeforeIDs = examplesBefore.map(function(example) {
      return example._id;
    });
    if (skeletonEntropy == -1000) {
      var fetched = examplesBefore.fetch();
      var skeletonLabelledPositiveMatches = fetched.filter(function(example) {
        return example['label'] === 'positive';
      });
      var skeletonLabelledNegativeMatches = fetched.filter(function(example) {
        return example['label'] === 'negative';
      });

      var allLabelledExamples = fetchShortestExamples(constructSelectorToFilterBaggedPatterns( {label: {'$in': ['positive', 'negative']}} )).fetch();

      skeletonEntropy = entropy(skeletonLabelledPositiveMatches, skeletonLabelledNegativeMatches, 0.5);
    }

    subgraphLabelledPositiveMatches[text] = mapping[text] ? 
      mapping[text].filter(function(example) {
        return example['label'] === 'positive';
      }) : 
      [];
    subgraphLabelledNegativeMatches [text] = mapping[text] ?
      mapping[text].filter(function(example) {
        return example['label'] === 'negative';
      }) : 
      [];

      var allLabelledExamples = fetchShortestExamples(constructSelectorToFilterBaggedPatterns( {label: {'$in': ['positive', 'negative']}} )).fetch();


    subgraphEntropy[text] = mapping[text] ? 
      entropy(subgraphLabelledPositiveMatches[text], subgraphLabelledNegativeMatches [text], skeletonEntropy) : 
      -1;

    var matchedExampleIds = mapping[text] ? mapping[text].map(function(example) {
      return example.exampleID;
    }) : [];

    subgraphUnmatchedEntropy[text] = mapping[text] ?
      entropy(allLabelledExamples.filter (function(example) {
        return example['label'] === 'positive' && !matchedExampleIds.includes(example.exampleID);
      }), 
      allLabelledExamples.filter (function(example) {
        return example['label'] === 'negative' && !matchedExampleIds.includes(example.exampleID);
      }), 0.5) :
      -1;



    var examplesRemovedIDs = _.difference(examplesBeforeIDs, examplesAfterIDs);
    examplesBefore.fetch()
    .filter(function(example) {
      return examplesRemovedIDs.indexOf(example._id) !== -1;
    })
    .forEach(function(example) {
      negativeMapping[text] = negativeMapping[text] || [];
      example.showAsRemoved = true;

      // check if mapping[text] already has example with the same id
      var hasExample = false;
      negativeMapping[text].forEach(function(existingExample) {
        if (existingExample._id === example._id) {
          hasExample = true;
        }
      });
      if (!hasExample) {
        negativeMapping[text].push(example);
      }

    });
  });

  // sort all values in  mapping  and negativeMapping
  // each example should be sorted such that the positive examples are first, and the negative examples are next, then unlabelled examples

  for (var key in mapping) {
    sortExamples(mapping[key]);
  }
  for (var key in negativeMapping) {
    sortExamples(negativeMapping[key]);
  }


  var listOfElements = [];
  var headerCount = 0;


  var informativenessWeight = Session.get('informativenessWeight');
  var representativenessWeight = Session.get('representativenessWeight');
  var diversityWeight = Session.get('diversityWeight');

  console.log('informativenessWeight', informativenessWeight);
  console.log('representativenessWeight', representativenessWeight);

  // console.log('[clusteredExamples mapping');
  // console.log(mapping);
  for (var key in mapping) {
    var labelledPositiveAndMatchingCount = (mapping[key] || []).filter(function(example) {
      return example['label'] === 'positive';
    }).length
    var labelledPositiveAndNotMatchingCount = (negativeMapping[key] || []).filter(function(example) {
      return example['label'] === 'positive';
    }).length;

    var labelledNegativeAndMatchingCount = (mapping[key] || []).filter(function(example) {
      return example['label'] === 'negative';
    }).length;

    var labelledNegativeAndNotMatchingCount =  (negativeMapping[key] || []).filter(function(example) {
      return example['label'] === 'negative';
    }).length;

    var informationGainDenominator =  labelledPositiveAndMatchingCount + labelledPositiveAndNotMatchingCount + labelledNegativeAndMatchingCount + labelledNegativeAndNotMatchingCount;

    var averageEntropy = (labelledPositiveAndMatchingCount + labelledNegativeAndMatchingCount) / informationGainDenominator * subgraphEntropy[key] + ( (labelledPositiveAndNotMatchingCount + labelledNegativeAndNotMatchingCount) / informationGainDenominator * subgraphUnmatchedEntropy[key]) ;
    var informationGain = skeletonEntropy - averageEntropy;
    if (!(informationGain > 0)) {
      informationGain = 0;
    }

    var representativeness = (mapping[key] || []).length;

    var hasFeedback = hastoolFeedback[ key ];

    // normalize the informationgain and representativeness
    // informationGain = informationGain ;
    representativeness = representativeness / totalExamples;
    
    listOfElements.push({headerCount: headerCount, header: headerText[key], 
      node: headerText[key].rawText,
      // while we call it "positive", this means "matching"
      positiveExamples: (mapping[key] || []), //.slice(0, 3) ,  
      positiveExampleCount: (mapping[key] || []).length,

      // while we call it "negative", this means "not matching"
      negativeExamples: (negativeMapping[key] || []), //.slice(0, 3),
      negativeExampleCount: (negativeMapping[key] || []).length,

      labelledPositiveAndMatchingCount: labelledPositiveAndMatchingCount,
      labelledPositiveAndNotMatchingCount: labelledPositiveAndNotMatchingCount,

      labelledNegativeAndMatchingCount: labelledNegativeAndMatchingCount,
      labelledNegativeAndNotMatchingCount: labelledNegativeAndNotMatchingCount,

      unlabelledAndMatchingCount: (mapping[key] || []).filter(function(example) {
        return example['label'] === '?';
      }).length,

      unlabelledAndNotMatchingCount: (negativeMapping[key] || []).filter(function(example) {
        return example['label'] === '?';
      }).length,

      informationGain: informationGain,
      representativeness: representativeness,
      score: informativenessWeight * (informationGain) + representativenessWeight * representativeness,
      toolFeedback: hasFeedback,

      // debug
      subgraphEntropy: subgraphEntropy[key],
      subgraphUnmatchedEntropy: subgraphUnmatchedEntropy[key],
      skeletonEntropy: skeletonEntropy,
    });

    headerCount += 1;
  }



  // sort listOfElements 
  listOfElements.sort(function(a, b) {
    // return b.informationGain - a.informationGain;
    // return b.positiveExampleCount  + b.negativeExampleCount - a.positiveExampleCount - a.negativeExampleCount;

    return b.score - a.score > 0 ? 1 : -1;
  });
  
  // console.log('listOfElements');
  // console.log(listOfElements);

  var elementRoles = identifyElementRoles();

  var exampleClustersByRole = {};
  
  listOfElements
    .filter(function(element) {
      return elementRoles[element.node];
    })
    .forEach(function(element) {
      var roles = elementRoles[element.node];
      roles.forEach(function(role) {

        if (role == 'guard' && exampleClustersByRole[role] && exampleClustersByRole[role].length >= 3 && !element.toolFeedback) {
          return;
        }

        // TODO: here is a temp hack to hide the methods that are parameters. This simplifies the interface significantly for the user
        // if (roleList[3]) {
        //   roleList[3].cluster = roleList[3].cluster.filter(function(cluster) {
        //     return !cluster.node.includes('(');
        //   });
        // }
        if (role == 'parameter1' && elementRoles[element.node].includes('method')) {
          return;
        }

        if (exampleClustersByRole[role] && exampleClustersByRole[role].length >= 3 && !getFocalNode().includes(element.node)&& !element.toolFeedback ) {
          return;
        }

        // for 'guard', do not add (order)
        if (role === 'guard' && element.header && element.header.edge && element.header.edge.label && element.header.edge.label.indexOf('(order)') !== -1) {
          return;
        }

        exampleClustersByRole[role] = exampleClustersByRole[role] || [];
        exampleClustersByRole[role].push(element);
        
        if (element.toolFeedback ) {

          // TODO check if another element matches a superset of the the labelled examples 
          var hasAnotherElementWithSuperset 

          var elementsWithSuperset = 
            listOfElements
              .filter(function(anotherElement) {
                // we pick only another element that is not the same as the current element
                return anotherElement.node !== element.node;
              })
              .filter(function(anotherElement) {
                // console.log('anotherElement', anotherElement);
                // console.log(subgraphLabelledPositiveMatches[anotherElement.header.edge.rawText]);
                // console.log('element', element);
                // console.log(subgraphLabelledPositiveMatches[element.header.edge.rawText]);
                return elementRoles[anotherElement.node] 
                  && anotherElement.toolFeedback == 'bg-success'
                  && subgraphLabelledPositiveMatches[anotherElement.header.edge.rawText].length >= subgraphLabelledPositiveMatches[element.header.edge.rawText].length;
              })
              .filter(function(anotherElement) {
                var anotherElementExamples = subgraphLabelledPositiveMatches[anotherElement.header.edge.rawText].map(function(example) {
                  return example.exampleID;
                });
                var elementExamples = subgraphLabelledPositiveMatches[element.header.edge.rawText].map(function(example) {
                  return example.exampleID;
                });
                return _.difference(elementExamples, anotherElementExamples).length == 0;
              });
          var hasAnotherElementWithSuperset = elementsWithSuperset.length > 0;
          var elementsWithHintAndSuperset = elementsWithSuperset.filter(function(anotherElement) {
            return anotherElement.header.edge.rawText.replaceAll('.', '__') in subgraphWithHints;
          });
          var oneElementWithHintAndSuperset = elementsWithHintAndSuperset.length > 0 ? elementsWithHintAndSuperset[0] : undefined;
          
          var comparedElement = oneElementWithHintAndSuperset; // exampleClustersByRole[role].length > 1  ? exampleClustersByRole[role][0] : undefined;
          if (comparedElement && comparedElement.score > element.score) {
            // change feedback to the css class
            element.toolFeedback = 'bg-danger' 
            element.whatIf = '<p class="text-white" style="text-decoration: underline; cursor: pointer;">Why?</p>'
            
            element.whatifSubgraphA = comparedElement.node;
            element.whatifSubgraphB = element.node;
            element.whatifSubgraphAColor = 'green';
            element.whatifSubgraphBColor = 'red';

            var ignoreSubgraphs = window.ignoreSubgraphs;
            ignoreSubgraphs = ignoreSubgraphs + ',' + element.node;
            window.ignoreSubgraphs = ignoreSubgraphs;
          } else {
            if (hasAnotherElementWithSuperset) {
              element.toolFeedback = 'bg-warning';
              // element.whatIf = '<p style="text-decoration: underline; cursor: pointer;">Why?</p>'
              element.whatifSubgraphA = oneElementWithHintAndSuperset ? oneElementWithHintAndSuperset : elementsWithSuperset[0].node;
              element.whatifSubgraphB = element.node;
              element.whatifSubgraphAColor = 'green';
              element.whatifSubgraphBColor = 'yellow';

              var ignoreSubgraphs = window.ignoreSubgraphs;
              ignoreSubgraphs = ignoreSubgraphs + ',' + element.node;
              window.ignoreSubgraphs = ignoreSubgraphs;
              
            } else {
              element.toolFeedback = 'bg-success' 
            }
          }
          
        } else {
          // if ignoreSubgraphs, remove the element from the list
          if (window.ignoreSubgraphs && window.ignoreSubgraphs.includes(element.node)) {
            var ignoreSubgraphs = window.ignoreSubgraphs.split(',');
            ignoreSubgraphs = ignoreSubgraphs.filter(function(item) {
              return item !== element.node
            });
            window.ignoreSubgraphs = ignoreSubgraphs.join(',');
          }
        }
        
      });

    });
    // console.log('exampleClustersByRole');
  // console.log(exampleClustersByRole);

  // enumerate over exampleClustersByRole
  var displayedElementCount=0;
  for (var clusterKey of ['declaration', 'guard', 'parameter1', 'method', 'exception', 'return',  'error', undefined]) {
    if (!exampleClustersByRole[clusterKey]) {
      continue;
    }
    var clusters = exampleClustersByRole[clusterKey];
    clusters.forEach(function(cluster, cluster_i) {
      cluster['rowNumber'] = displayedElementCount;
      displayedElementCount += 1;
    });
  }

  // as list
  var keyColors = {
    'declaration': '120',
    'guard': '160',
    'method': '200',
    'parameter1': '200',
    'exception': '260',
    'return': '220',
    'error': '280',
  }
  exampleClustersByRole['guard'] = []; // quick hack to suppress ugly pattern
  var roleList = [];
  for (var key of ['declaration', 'guard', 'parameter1', 'method', 'exception', 'return',  'error', undefined]) {
    var add = {
      role: key,
      cluster: structuredClone(exampleClustersByRole[key]),
      contextColor: keyColors[key],
    };
    roleList.push(add);

    // the examples will go under the cluster of the role
    if (!add.cluster) {
      continue;
    }
    add.cluster.forEach(function(cluster, cluster_i) {
      cluster.positiveExamples.forEach(function(example) {
        example['clusterContext'] = key + '-add-' + cluster.node.replaceAll('(', '').replaceAll(')', '').replaceAll('<', '').replaceAll('>', '').replaceAll(':', '').replaceAll('/', '').replaceAll('[', '').replaceAll(']', '').replaceAll('=', '');
        example['additionalNode'] = cluster.node;
      });
      cluster.negativeExamples.forEach(function(example) {
        example['clusterContext'] = key + '-removed-' + cluster.node.replaceAll('(', '').replaceAll(')', '').replaceAll('<', '').replaceAll('>', '').replaceAll(':', '').replaceAll('/', '').replaceAll('[', '').replaceAll(']', '').replaceAll('=', '');
      });

      cluster['contextColor'] = keyColors[key] - cluster_i;
    });
    if (add.cluster) {
      add.cluster.forEach(function(cluster) {
        cluster['role'] = key;
      });
    }
    add.cluster.forEach(function(cluster) {
      Meteor.defer(function() {
        cluster.positiveExamples.concat(cluster.negativeExamples).forEach(function(example) {
          render(example);
        });
      });
    });
    
  }

  // within the `method` role, sort the clusters
  if (roleList[roleList.indexOf('method')]) {

    // insert "nodes" corresponding to the skelton
    var subgraphNodeLabels = 
        Object.keys(skeleton)
        .filter(function(nodeLabel) {
          var checked = skeleton[nodeLabel].checked;
          return checked;
        })
        .filter(function(nodeLabel) {
          return !nodeLabel.includes('->') && nodeLabel.includes('('); // looks like a method call
        })
        .map(function(nodeLabel) {
          return nodeLabel;
        });

    

    // determine which nodes are positioned behind each subgraphNodeLabel

    // sort the clusters
    var beforeNodeLabels = {};

    subgraphNodeLabels.forEach(function(nodeLabel) {
      beforeNodeLabels[nodeLabel] = [];
      roleList[1].cluster.forEach(function(one_subgraph) {
        if (one_subgraph.header.otherNode === nodeLabel && !one_subgraph.header.linkedToSource) {
          
          beforeNodeLabels[nodeLabel].push(one_subgraph);
        }
      });        

      Object.keys(skeleton).filter(element => element.includes('->')).forEach(function(skeletonEdge) {
        var skeletonEdgeSource = skeletonEdge.split(' -> ')[0];
        var skeletonEdgeTarget = skeletonEdge.split(' -> ')[1].split(' ')[0];

        // check both are method calls
        if (!skeletonEdgeSource.includes('(') || !skeletonEdgeTarget.includes('(')) {
          return;
        }

        if (skeletonEdgeTarget === nodeLabel) {
          var otherNode = skeletonEdgeSource;

          beforeNodeLabels[nodeLabel].push(otherNode);

        }

      });
    });

    // sort the subgraphNodeLabels by length of beforeNodeLabels
    var sortedNodeLabels = subgraphNodeLabels.sort(function(a, b) {
      return beforeNodeLabels[a].length - beforeNodeLabels[b].length;
    });

    var newCluster = []
    var alreadyInserted = {};
    // insert the clusters in the sorted order
    sortedNodeLabels.forEach(function(nodeLabel) {
      beforeNodeLabels[nodeLabel].forEach(function(subgraph) {
        if (!subgraph.node || alreadyInserted[subgraph.node]) {
          return;
        }
        newCluster.push(subgraph);
        alreadyInserted[subgraph.node] = true;
      });
      // insert subgraphNodeLabels into the cluster
      newCluster.push({
        header: {
          pseudo: true,
          edge: {
            rawText: nodeLabel
          }
        },
        node: nodeLabel,
        positiveExamples: [],
        positiveExampleCount: 0,
        negativeExamples: [],
        negativeExampleCount: 0,
        role: 'method',
        rawText : nodeLabel
      });
    });
    // append remaining subgraphs in cluster 
    roleList[1].cluster.forEach(function(subgraph) {
      if (alreadyInserted[subgraph.node]) {
        return;
      }
      newCluster.push(subgraph);
    });

    roleList[1].cluster = newCluster;;
  }

  return roleList;
}

function patternGrowingCandidateNodes(subgraphWithHints) { // TODO, we currently use a dumb method for matching the nodes. Have to upgrade to graph matching
  // find subgraphs already in the skeleton
  // buildSkeleton();
  var skeleton = Session.get('skeleton');
  var nodesInSkeleton = 
  _.filter(skeleton, function(node) {
    return node.checked;
  }).map(function(node) {
    return node.text.replaceAll('.', '__');
  });


  
  if (_.isEmpty(nodesInSkeleton)) {
    var allPossibleNodes = allNodes();
    var nodesNotInSkeleton = _.difference(allPossibleNodes, nodesInSkeleton);

    return nodesNotInSkeleton
    .filter(function(node) {
      // hide the UNKNOWNs
      return !node.includes('UNKNOWN');
    })  
    .filter(filterOnlyLiteralsAndExceptionsAndCalls)
    .map(function(node) {

      if (subgraphWithHints[node]) {
        return { rawText: node, toolFeedback: true };
      } else {
        return { rawText: node };
      }
    });
  } else {
      // also always consider <catch> 
    nodesInSkeleton.push('<catch>');

    var allPossibleEdges = [];
    // get nodes that are connected
    var alreadyAdded = new Set();
    Subgraphs.find({}).forEach(function(subgraph) {

      subgraph.edges.forEach(function(edge) {
        if (nodesInSkeleton.includes(edge.from)) {

          if (!alreadyAdded.has(edge.to)) {
            edge.linkedToSource = true;
            allPossibleEdges.push(edge);
            alreadyAdded.add(edge.to);
          }
        }
        else if (nodesInSkeleton.includes(edge.to)) {
          if (!alreadyAdded.has(edge.from)) {
            edge.linkedToSource = false;
            allPossibleEdges.push(edge);
            alreadyAdded.add(edge.from);
          }
        }
        
      });
    });

    return allPossibleEdges.map(function(edge) {
      var node  = edge.linkedToSource ? edge.to : edge.from;
      var otherNode = edge.linkedToSource ? edge.from : edge.to;

      var hasHint = subgraphWithHints[edge.rawText.replaceAll(".", "__")];
      return { rawText: node, composite:true, linkedToSource: edge.linkedToSource, edge: edge, otherNode: otherNode, toolFeedback: hasHint };
    })
    .filter(function(node) {
      // filter stuff already in the skeleton
      return !nodesInSkeleton.includes(node.rawText);
    })
    .filter(function(node) {
      // hide the UNKNOWNs
      return !node.rawText.includes('UNKNOWN');
    })  
    .filter(function(node) {

      // filter stuff that is not a literal or a call
      
      return filterOnlyLiteralsAndExceptionsAndCalls(node.rawText);
    });
  }
}

function alternativeSubgraphsCandidateNodes(subgraphWithHints) { // TODO, we currently use a dumb method for matching the nodes. Have to upgrade to graph matching
  // find subgraphs already in the skeleton
  // buildSkeleton();

  // find altnerative
  var subgraphs = Subgraphs.find({ '$and': [{ 'alternative': true },] }).fetch();

  var possibleEdges = [];
    // get nodes that are connected
    var alreadyAdded = new Set();
  subgraphs.forEach(function(subgraph) {
    subgraph.edges.forEach(function(edge) {

        if (!alreadyAdded.has(edge.to)) {
          edge.linkedToSource = true;
          possibleEdges.push(edge);
          alreadyAdded.add(edge.to);
        }
      
        if (!alreadyAdded.has(edge.from)) {
          edge.linkedToSource = false;
          possibleEdges.push(edge);
          alreadyAdded.add(edge.from);
        }
      
      
    });
  });
  return possibleEdges.map(function(edge) {
    var node  = edge.linkedToSource ? edge.to : edge.from;
    var otherNode = edge.linkedToSource ? edge.from : edge.to;

    var hasHint = subgraphWithHints[edge.rawText.replaceAll(".", "__")];
    return { rawText: node, composite:true, linkedToSource: edge.linkedToSource, edge: edge, otherNode: otherNode, toolFeedback: hasHint };
  })
  .filter(function(node) {
    // hide the UNKNOWNs
    return !node.rawText.includes('UNKNOWN');
  })  
  .filter(function(node) {

    // filter stuff that is not a literal or a call
    
    return filterOnlyLiteralsAndExceptionsAndCalls(node.rawText);
  });
}

function subgraphsToDisplay(viewType) {
  var hasPositiveLabelledExamples = Examples.find({ label: 'positive' }).count() > 0;
  var hasNegativeLabelledExamples = Examples.find({ label: 'negative' }).count() > 0;


  if ([//'all', 
      'unlabelled', 'matching'].includes(viewType) || (viewType === 'labelled' && (!hasPositiveLabelledExamples || !hasNegativeLabelledExamples))) {
    var subgraphs = Subgraphs.find({ '$and': [{ 'discriminative': false, 'alternative': { '$ne': true } }, { '$or': [{ 'bags': { '$exists': false } }, { 'bags': { $eq: null } }] }] }).fetch();

    console.log('suggested subgraphs by finding patterns distinguishing unlabelled examples from labelled examples', subgraphs);

    // filter out "MISSING"
    subgraphs = subgraphs.filter(subgraph => subgraph.rawText !== "MISSING")
      .filter(subgraph => filterOnlyLiteralsAndCalls(subgraph.rawText));
  } else {
    var subgraphs = Subgraphs.find({ '$and': [{ 'alternative': true },] }).fetch();

    console.log('suggested subgraphs by finding alternative patterns', subgraphs);

    // console.log(' + frequent');
    // var moreSubgraphs = Subgraphs.find({ '$and': [{ 'discriminative': false, 'alternative': { '$ne': true } }, { '$or': [{ 'bags': { '$exists': false } }, { 'bags': { $eq: null } }] }] }).fetch();

    // append moreSubgraphs to 
    // subgraphs = subgraphs.concat(moreSubgraphs);

    // filter out "MISSING"
    subgraphs = subgraphs.filter(subgraph => subgraph.rawText !== "MISSING")
      .filter(subgraph => filterOnlyLiteralsAndCalls(subgraph.rawText));
  }
  return subgraphs;
}

function escapeNodeForDisplay(text) {
  if (text == undefined) return '';
  var result = text.replaceAll('__', '.')
    // .replaceAll("UNKNOWN", "<expr>");
    .replaceAll("UNKNOWN.", "");
  if (result.includes("String:")) {
    result = '' + result.replaceAll("String:", '"') + '"';
  }
  if (result.includes("int:")) {
    result = '' + result.replaceAll("int:", '') + '';
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
    result = '<span class="hljs-keyword">' +  "new " + '</span>' + result.replaceAll(".<init>", "(...)");
  }
  if (result.includes("<cast>")) {
    result = "(" + result.replaceAll(".<cast>", ") ...");
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
    result = '... <span class="hljs-keyword">instanceof </span>' + result.replaceAll(".<instanceof>", "") + '';
  }
  return result;
}





function constructSelectorToFilterBaggedPatterns(selector) {
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

  // special case: under preview
  var previewBag = Session.get('previewBag');

  Object.keys(bagsToSubgraphs) // iterate over the bags
    .filter(function (bag) {
      return bag != previewBag;
    })
    .forEach(function (bag) {
      var subgraphIds = bagsToSubgraphs[bag];
      var subgraphs = Subgraphs.find({ _id: { '$in': subgraphIds } }).fetch();
      var subgraphsSelector = computeSelector(subgraphs);
      disjunct.push(subgraphsSelector);
    });

  // disjunct.push({   });
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

function constructSelectorToMatchBaggedPatterns(selector) {

  // if (_.isEmpty(selector)) {
  //   return {'dummy:': 'match nothing'};
  // }
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

  // special case: under preview
  var previewBag = Session.get('previewBag');

  var selector = {};
  Object.keys(bagsToSubgraphs) // iterate over the bags
    .filter(function (bag) {
      return bag != previewBag;
    })
    .forEach(function (bag) {
      var subgraphIds = bagsToSubgraphs[bag];
      var subgraphs = Subgraphs.find({ _id: { '$in': subgraphIds } }).fetch();
      var subgraphsSelector = computeSelector(subgraphs);
      disjunct.push(subgraphsSelector);
    });

  if (selector['$or'] != undefined) {
    if (disjunct.length > 0) {
      selector['$or'].push( disjunct );
    }
  } else {
    if (disjunct.length > 0) {
      selector['$or'] = disjunct;
    }
  }
  return selector;
}

function computeSelectorWithAdditionalSubgraphByText(skeleton, subgraphText) {
  var selector = computeSelectorFromSkeleton(skeleton);

  subgraphText = subgraphText.replace(/\./g, '__') ;
  
  var singleSubgraphSelector = {
    '$and': [{}]
  };
  singleSubgraphSelector['$and'][0][subgraphText] = {'$exists': true}
  return combineSelectorWithSingleSubgraphSelector(selector, singleSubgraphSelector);
}

function computeSelectorWithOnlySubgraphByText(skeleton, subgraphText) {

  subgraphText = subgraphText.replace(/\./g, '__') ;
  
  var singleSubgraphSelector = {
    '$and': [{}]
  };
  singleSubgraphSelector['$and'][0][subgraphText] = {'$exists': true}
  
  return singleSubgraphSelector;
}


function computeSelectorWithAdditionalSubgraph(skeleton, subgraphId) {
  var selector = computeSelectorFromSkeleton(skeleton);
  var subgraphs = [Subgraphs.findOne({ _id: subgraphId })];
  if (subgraphs[0] == undefined) return '';
    
  var singleSubgraphSelector = computeSelector(subgraphs);

  
  // combine the selectors
  return combineSelectorWithSingleSubgraphSelector(selector, singleSubgraphSelector);
}

function combineSelectorWithSingleSubgraphSelector(selector, singleSubgraphSelector) {
  var newConjunction = [];
  if (selector['$and']) {
    newConjunction = selector['$and'];
  }

  for (var key in singleSubgraphSelector['$and']) {
    var subgraphSelector = singleSubgraphSelector['$and'][key];
    var found = false;
    for (var key2 in newConjunction) {
      var conjunctionSelector = newConjunction[key2];
      if (_.isEqual(Object.keys(subgraphSelector), Object.keys(conjunctionSelector))) {
        found = true;

        // overwrite
        newConjunction[key2] = subgraphSelector;

        break;
      }
    }
    if (!found) {
      newConjunction.push(subgraphSelector);
    }
  }

  selector['$and'] = newConjunction;
  return selector;
}

function computeWidth(selector) {
  var conditionalPositiveMatches = fetchAndCountLabelledExamples(selector, 'positive');
  var conditionalPositiveCount = conditionalPositiveMatches.match;
  var conditionalPositiveMisses = conditionalPositiveMatches.missed;
  var conditionalNegativeMatches = fetchAndCountLabelledExamples(selector, 'negative');
  var conditionalNegativeCount = conditionalNegativeMatches.match;
  var conditionalNegativeMisses = conditionalNegativeMatches.missed;

  // ideally, we distinguish between all positive and negative examples
  var conditionalCount = conditionalPositiveCount + conditionalNegativeMisses - conditionalPositiveMisses - conditionalNegativeCount;

  return (100 * conditionalCount / optimalLabellingTotal());
}


function computePositiveWidth(selector) {
  if (Object.keys(selector) == 0) {
    return 0;
  }

  var skeleton = Session.get('skeleton');
  var examplesInViewSelector = computeSelectorByViewAndKeyword(skeleton);
  examplesInViewSelector = constructSelectorToFilterBaggedPatterns(examplesInViewSelector);

  if (examplesInViewSelector['$and']) {
    var mergedSelectorConjunction = selector['$and'].concat(examplesInViewSelector['$and']);
    var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
  } else {
    var mergedSelector = selector;
  }

  var conditionalPositiveMatches = fetchAndCountLabelledExamples(mergedSelector, 'positive');
  var conditionalPositiveCount = conditionalPositiveMatches.match;

  var idealSelector = constructSelectorToFilterBaggedPatterns(computeSelectorByViewAndKeyword(skeleton))
  var idealMatches = fetchAndCountLabelledExamples(idealSelector, 'positive');

  return (100 * conditionalPositiveCount / idealMatches.match);
}

function computeNegativeWidth(selector) {
  if (Object.keys(selector) == 0) {
    return 0;
  }

  var skeleton = Session.get('skeleton');
  var examplesInViewSelector = computeSelectorByViewAndKeyword(skeleton);
  examplesInViewSelector = constructSelectorToFilterBaggedPatterns(examplesInViewSelector);

  if (examplesInViewSelector['$and']) {
    var mergedSelectorConjunction = selector['$and'].concat(examplesInViewSelector['$and']);
    var mergedSelector = { '$and': structuredClone(mergedSelectorConjunction) };
  } else {
    var mergedSelector = selector;
  }

  var conditionalNegativeMatches = fetchAndCountLabelledExamples(mergedSelector, 'negative');
  var conditionalNegativeCount = conditionalNegativeMatches.match;

  var idealSelector = constructSelectorToFilterBaggedPatterns(computeSelectorByViewAndKeyword(skeleton))
  var idealMatches = fetchAndCountLabelledExamples(idealSelector, 'negative');


  return (100 * conditionalNegativeCount / idealMatches.match);
}




/*
 * Below is the helper functions for the general template of code blocks and options.
 */
/*
Template.optionBlock.helpers({
  conditionalCountBlock(blockname){
    var selector = Session.get('selector');
    console.log('selector in conditionalCountBlock',selector);
    if ( _.isEmpty(selector) ) {
      return 0; 
    } else {
      //if we're not already filtering for an element in this block
      //then filter for it
      if (!selector[blockname]) {
        //is it a checkbox-block or a radio-button block?
        if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
          selector[blockname] = {$ne: []};
        } else {
          selector[blockname] = {$ne: 'empty'};
        }
      }
      var conditionalCount = fetchAndCountExamples(selector);
      return conditionalCount;
    }
  },
  conditionalCountOption(optionname){
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return 0;
    } else {
      //is it a radio or check button?
      if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
        //it is a check box
        var block_selector_object = selector[blockname];
        if (_.isEmpty(block_selector_object) || _.isEqual(block_selector_object,{$ne: []})){
          var updated_list = [ optionname ];
        } else if (!_.isEmpty(block_selector_object['$all'])) {
          var list_of_options = block_selector_object['$all'];
          var updated_list = list_of_options.concat([ optionname ]);
        } else {
          console.log('unhandled case!')
        }
        selector[blockname] = { '$all': updated_list };
        return fetchAndCountExamples(selector);
      } else {
        if (!_.isEmpty(selector[blockname]) && !_.isEqual(selector[blockname],{$ne: 'empty'}) && selector[blockname] !== optionname ){
          //then another radio button is selected; dont override it
          return 0 
        } else {
          selector[blockname] = optionname;
          return fetchAndCountExamples(selector);
        }
      }
    }
  },
  countBelowThreshold(optionname){
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    console.log('countBelowThreshold',blockname,optionname);

    var selector = Session.get('selector');

    if ( _.isEmpty(selector) ) {
      return exampleTotal() <= Session.get('countTooLowThreshold');
    } else {
      //is it a radio or check button?
      if (!_.isEmpty(_.find(option_lists, function(list_type){ return list_type===blockname;}))){
        //it is a check box
        var block_selector_object = selector[blockname];
        if (_.isEmpty(block_selector_object) || _.isEqual(block_selector_object,{$ne: []})){
          var updated_list = [ optionname ];
        } else if (!_.isEmpty(block_selector_object['$all'])) {
          var list_of_options = block_selector_object['$all'];
          var updated_list = list_of_options.concat([ optionname ]);
        } else {
          console.log('unhandled case!')
        }
        selector[blockname] = { '$all': updated_list };
        return fetchAndCountExamples(selector) <= Session.get('countTooLowThreshold');
      } else {
        if (!_.isEmpty(selector[blockname]) && !_.isEqual(selector[blockname],{$ne: 'empty'}) && selector[blockname] !== optionname ){
          //then another radio button is selected; dont override it
          return 0 <= Session.get('countTooLowThreshold');
        } else {
          selector[blockname] = optionname;
          return fetchAndCountExamples(selector) <= Session.get('countTooLowThreshold');
        }
      }
    }
  },
  conditionalCountTooLow(optionname){
    if (Session.get('toggleZeroCount')) {
      var parentData = Template.parentData();
      var blockname = parentData['blockname'];
      var selector = Session.get('selector');
      if ( _.isEmpty(selector) ) {
        return false;
      } else {
        //TODO: CHECK FOR RADIO BUTTON OR CHECKBOX SEE conditionalCountOption
        if (!_.isEmpty(selector[blockname]) && !_.isEqual(selector[blockname],{$ne: 'empty'}) && selector[blockname] !== optionname ){
          //then another radio button is selected; dont override it
          return true;
        } else {
          selector[blockname] = optionname;
          var conditionalCount = fetchAndCountExamples(selector);
          return conditionalCount <= 0;
        }
      }
    } else { //TODO: TEST IF CONDITIONALCOUNT <= Session.get('countTooLowThreshold');
      return false;
    }
  },
  count(optionname) {
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    var selector = {};
    selector[blockname] = optionname;
    return fetchAndCountExamples(selector);
  },
  width(optionname) {
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    var selector = {};
    selector[blockname] = optionname;
    var count = fetchAndCountExamples(selector);
    return (100*count/exampleTotal()).toFixed(2);
  },
  opacity(optionname) {
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];

    var selector = {};
    selector[blockname] = optionname;
    var count = fetchAndCountExamples(selector);
    var selector = Session.get('selector');
    if ( _.isEmpty(selector) ) {
      return Math.log(1+count/exampleTotal()).toFixed(2);
    } else {
      selector[blockname] = optionname;
      var conditionalCount = fetchAndCountExamples(selector);
      return Math.log(1+conditionalCount/exampleTotal()).toFixed(2);
    }
  },
  codeSnippet(blockname) {
    switch(blockname) {
      // case 'try':
      //   return 'try {';
      //   break;
      case 'exceptionType':
        return '} catch ... {';
        break;
      case 'cleanUpCall':
        return '} finally {';
        break;
      default:
        return '';
    }
  },
  postCodeSnippet(blockname) {
    switch(blockname) {
      case 'cleanUpCall':
        return '}';
        break;
      case 'use':
        return '}';
        break;
      default:
        return '';
    }
  },
  postIndentationOuter(blockname) {
    switch(blockname) {
      case 'exceptionType':
        return '10';
        break;
      case 'use':
        return '30';
        break;
      default:
        return '';
    }
  },
  preCodeSnippet(blockname) {
    switch(blockname) {
      case 'exceptionType':
        return '}';
        break;
      default:
        return '';
    }
  },
  preIndentationOuter(blockname){
    switch(blockname) {
      case "exceptionType":
        return 30;
        break;
      default:
        return '';
    }
  },
  prepreIndentationOuter(blockname){
    switch(blockname) {
      case "exceptionType":
        return 50;
        break;
      default:
        return '';
    }
  },
  indentationInner() {
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    switch(blockname) {
      case "initialization":
        return 0;
        break;
      case "try":
        return 0;
        break;
      case "configuration":
        return 20;
        break;
      case 'guardType':
        return 20;
        break;
      case 'guardCondition':
        return 40;
        break;
      case 'focalAPI':
        return 60;
        break;
      case 'checkType':
        return 60;
        break;
      case 'followUpCheck':
        return 80;
        break;
      case 'use':
        return 100;
        break;
      case 'exceptionType':
        return 20;
        break;
      case 'exceptionHandlingCall':
        return 40;
        break;
      // case 'finally':
      //   return 0;
      //   break;
      case 'cleanUpCall':
        return 40;
        break;
      default:
        return 0;
    }
  },
  indentationOuter(blockname) {
    switch(blockname) {
      case "initialization":
        return 0;
        break;
      case "try":
        return 0;
        break;
      case "configuration":
        return 20;
        break;
      case 'guardType':
        return 20;
        break;
      case 'guardCondition':
        return 40;
        break;
      case 'focalAPI':
        return 60;
        break;
      case 'checkType':
        return 60;
        break;
      case 'followUpCheck':
        return 80;
        break;
      case 'use':
        return 100;
        break;
      case 'exceptionType':
        return 20;
        break;
      case 'exceptionHandlingCall':
        return 40;
        break;
      // case 'finally':
      //   return 0;
      //   break;
      case 'cleanUpCall':
        return 40;
        break;
      default:
        return 0;
    }
  },
  singleOptionClass(){
    var parentData = Template.parentData();
    var blockname = parentData['blockname'];
    if (blockname=="focalAPI" || blockname=="finally") {
      return "singleOption";
    }
  },
  visibility(blockname) {
    if (blockname=="focalAPI" || blockname=="finally") {
      return "hidden";
    }
    return "inherit";
  },
}); */
