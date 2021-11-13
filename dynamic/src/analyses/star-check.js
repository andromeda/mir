let env;

const uniqueValid = new Set();
const uniqueInvalid = new Set();
const path = require('path');
const fs = require('fs');
const starSet = new Set();

let countValid = 0;
let countInvalid = 0;
let groundTruth;
let debug = false;

// We create a set that contains all the names of *
const setStar = (data) => {
  Object.keys(data).forEach((name) => {
    Object.keys(data[name]).forEach((item) => {
      if (item.includes('.*')) {
        const base = item.split('*')[0];
        starSet.add(base);
      } else if (item.includes('*.')) {
        const base = item.split('*')[1];
        starSet.add(base);
      }
    });
  });
  return data;
};

const hasStar = (name) => {
  for (const key of starSet) {
    if (name.includes(key)) {
      return true;
    }
  }
  return false;
};

// We need to get the path of the main module in order to find dynamic json
const getAnalysisData = () => {
  // We save all the json data inside an object
  const appDir = env.conf.rules? env.conf.rules :
    path.join(path.dirname(require.main.filename), 'correct.json');
  let dynamicData;
  try {
    dynamicData = require(appDir);
  } catch (e) {
    throw new Error(appDir + ' file was not found!');
  }

  dynamicData = setStar(dynamicData);
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
      console.log(storedCalls, truename, modeGrid)
      if (Object.prototype.hasOwnProperty.
          call(storedCalls, truename) === false) {
        if (hasStar(truename)) {
          console.log('Valid access because of * on ' + truename);
          countValid++;
          return;
        }
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
    console.log(info.currentModule)
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

// onConstruct <~ Is call before every construct
const onConstruct = (info) => {
  checkRWX(groundTruth[info.currentName], info.nameToStore, ['r', 'x']);
};

// onExit (toSave == place to save the result) --maybe make it module-local?
const onExit = () => {
  const debugName = env.conf.debugName ? env.conf.debugName : '';
  const total = env.counters.total;
  const ratio = (+(countInvalid / total).toFixed(5));
  const corr = countValid > 0? 'correct' : '';
  const msg = `${debugName} ${total} ${uniqueValid.size} ${uniqueInvalid.size} ${countValid} ${countInvalid} ${ratio} ${corr}`;
  if (env.conf.print) {
    console.error(msg);
  }
  if (env.conf.SAVE_STATS) {
    fs.appendFileSync(env.conf.SAVE_STATS, msg + '\n', 'utf-8');
  }
};

module.exports = (e) => {
  env = e;
  groundTruth = getAnalysisData();
  debug = env.conf.debug;
  return {
    onRead: onRead,
    onCallPre: onCallPre,
    onWrite: onWrite,
    onConstruct: onConstruct,
    onExit: onExit,
  };
};
