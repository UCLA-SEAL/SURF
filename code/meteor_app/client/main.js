import '../imports/ui/body.js';
// import '../imports/ui/option.js';

// var APIshortName = 'digest';
// var API = 'java.security.MessageDigest__digest';
// var APIfocalNode = 'MessageDigest.getInstance()';

var APIshortName = "init";
var API = "javax.crypto.Cipher__init";
var APIfocalNode = "Cipher.getInstance()";

// var APIshortName = "random";
// var API = "java.security.SecureRandom__Key";
// var APIfocalNode = "SecureRandom.<init>";





Meteor.startup(function(){
    $.getScript('js/tutorons-library.js', function(){
        console.log('script should be loaded and do something with it.');
        spanAdder = new tutorons.TutoronsConnection(window);
        spanAdder.scanDom();
    });


    Session.set('countTooLowThreshold',0);
    Session.set('numOptions',3);
    Session.set('toggleZeroCount', true);
    Session.set('hideLabels', false);
    Session.set('hideOptions', false);
   $('.collapse').collapse('show');
    //  $('.collapse').show();  
  
    // var subjectNum = Number(prompt("Please enter your participant ID", "0"));
    
    var config = Config.findOne({});

    Session.set('subjectNum',-1); //update for each subject
    // Session.set('dataset', APIshortName); //'findViewById'); //update for each subject
    if (config) {
        Session.set('dataset', config.APIshortName); 
        // Session.set('focalAPI', 'Cipher__init()');
        // Session.set('focalAPI', API.split('.')[2] + '()');
        Session.set('focalAPI', config.API.split('.')[2] + '()');
        

        var API = config.API;
        if (API.includes('javax.crypto.Cipher__init')) {
            APIfocalNode = "Cipher.getInstance()";
        } else if (API.includes('java.security.MessageDigest__digest')) {
            APIfocalNode = "MessageDigest.getInstance()";
        } else if (API.includes('java.security.SecureRandom__Key')) {
            APIfocalNode = "SecureRandom.<init>";
        } 

        Session.set('focalNode', APIfocalNode);
    }

    Session.set('view', 'all');
    //create object which maps user id to correct dataset?

    // var participantId = Number(prompt("Please enter your participant ID", "0"));
    participantId = 0;
    // var datasets = {1: 'fileInputStream', 2: 'findViewById', 3: 'get', 4: 'query'};
    Session.set('subjectNum', participantId); //update for each subject

    // Session.set('dataset', datasets[datasetID]); //update for each subject
    Session.set('selector',{});


    Session.set('informativenessWeight', 0.5 );
    Session.set('representativenessWeight', 0.5 );
    Session.set('diversityWeight', 0.5 );
    
    // Session.set('ignoreSubgraphs', '');
    window.ignoreSubgraphs = '';

    console.log('session',Session.keys);

    Session.set('isBaseline', isBaseline);
   
    $('.open-label-query-examples-modal').click();
      
        
});
