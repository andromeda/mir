let env;

const uniqueValid = new Set();
const uniqueInvalid = new Set();
const path = require('path');
const fs = require('fs');
let countValid = 0;
let countInvalid = 0;
let groundTruth;
let debug = false;

// We need to get the path of the main module in order to find dynamic json
const getAnalysisData = () => {
  // We save all the json data inside an object
  const path = require('path');
  // TODO: Take this from env---relative to the file calling require('lya')!
  const appDir = path.join(path.dirname(require.main.filename), 'dynamic.json');
  let dynamicData;
  try {
    dynamicData = require(appDir);
  } catch (e) {
    throw new Error('The dynamic.json file was not found!');
  }
  return dynamicData;
};

// TODO: Make the path of the imported analysis result  not absolute
// etc.. /greg/home/lya/tst/main.js ~> main.js
const checkRWX = (storedCalls, truename, modeGrid) => {
  // This is a failback check
  if (!storedCalls) {
    return;
  }

  for (const key in modeGrid) {
    if (Object.prototype.hasOwnProperty.call(modeGrid, key)) {
      const mode = modeGrid[key];
      if (Object.prototype.hasOwnProperty.
          call(storedCalls, truename) === false) {
        console.log('Invalid access in: ' + truename + ' and mode ' + mode);
        uniqueInvalid.add(truename+mode);
        countInvalid++;
      } else {
        const permissions = storedCalls[truename];
        if (!permissions.includes(mode)) {
          console.log('Invalid access in: ' + truename + ' and mode ' + mode);
          uniqueInvalid.add(truename+mode);
          countInvalid++;
        } else {
          console.log('Valid access in: ' + truename + ' and mode ' + mode);
          uniqueValid.add(truename+mode);
          countValid++;
        }
      }
    }
  }
};

// Analyses provided by LYA.
// onRead <~ is called before every object is read
const onRead = (info) => {
  if (info.nameToStore != 'global') {
    const pattern = /require[(](.*)[)]/;
    if (pattern.test(info.nameToStore)) {
      checkRWX(groundTruth[info.currentModule],
          info.nameToStore.match(pattern)[0], ['r']);
    } else {
      checkRWX(groundTruth[info.currentModule],
          info.nameToStore.split('.')[0], ['r']);
    }
    checkRWX(groundTruth[info.currentModule],
        info.nameToStore, ['r']);
  }
};

// onWrite <~ is called before every write of an object
const onWrite = (info) => {
  checkRWX(groundTruth[info.currentModule], info.parentName, ['r']);
  checkRWX(groundTruth[info.currentModule], info.nameToStore, ['w']);
};

// onCallPre <~ is called before the execution of a function
const onCallPre = (info) => {
  if (info.typeClass === 'module-locals') {
    checkRWX(groundTruth[info.currentModule],
        'require', ['r', 'x']);
    checkRWX(groundTruth[info.currentModule],
        info.nameToStore, ['i']);
  } else {
    if (info.typeClass === 'node-globals') {
      checkRWX(groundTruth[info.declareModule],
          info.nameToStore.split('.')[0], ['r']);
    }
    checkRWX(groundTruth[info.declareModule],
        info.nameToStore, ['r', 'x']);
  }
};


// onExit (toSave == place to save the result) --maybe make it module-local?
const onExit = () => {
  const debugName = env.conf.debugName ? env.conf.debugName : '';
  const expl = '# name: (total, uniqueValid, uniqueInvalid, countValid, countInvalid, ratio, correctness)';
  const total = env.counters.total;
  const ratio = (+(countInvalid / total).toFixed(5));
  const corr = countValid > 0 ? 'correct' : '';
  const msg = `${debugName} ${total} ${uniqueValid.size} ${uniqueInvalid.size} ${countValid} ${countInvalid} ${ratio} ${corr} ${expl}`;
  if (env.conf.print) {
    console.error(msg);
  }
  if (env.conf.SAVE_STATS) {
    fs.appendFileSync(env.conf.SAVE_STATS, msg + '\n', 'utf-8');
  }
};

module.exports = (e) => {
  env = e;
  groundTruth = env.conf.rules? require(env.conf.rules) : getAnalysisData();
  debug = env.conf.debug;
  return {
    onRead: onRead,
    onCallPre: onCallPre,
    onWrite: onWrite,
    onExit: onExit,
  };
};
