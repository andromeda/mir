let env;
const pattern = /require[(](.*)[)]/;

// We add the R or W or E to the existing string
const addEvent = (event, values, index) => {
  let permissions = values[index];
  if (!permissions.includes(event)) {
    permissions += event;
    permissions = permissions.split('').sort().join('');
    values[index] = permissions;
  }
};

// Change the time parameters
const convert = (hrtime) => {
  const nanos = (hrtime[0] * 1e9) + hrtime[1];
  const millis = nanos / 1e6;
  const secs = nanos / 1e9;
  return {secs: secs, millis: millis, nanos: nanos};
};

// @storedCalls it is a table that contains all the analysis data
// @truename the name of the current function, object etc that we want to add to
// the table
// @mode the mode of the current access (R,W or E)
const updateAnalysisData = (storedCalls, truename, modeGrid) => {
  for (const key in modeGrid) {
    if (Object.prototype.hasOwnProperty.call(modeGrid, key)) {
      const mode = modeGrid[key];
      if (Object.prototype.hasOwnProperty.
          call(storedCalls, truename) === false) {
        storedCalls[truename] = mode;
      } else {
        addEvent(mode, storedCalls, truename);
      }
    }
  }
};

// Analyses provided by LYA.
// onRead <~ is called before every object is read
const onRead = (info) => {
  if (info.nameToStore !== 'global') {
    if (pattern.test(info.nameToStore)) {
      updateAnalysisData(env.analysisResult[info.currentModule],
          info.nameToStore.match(pattern)[0], ['r']);
    } else {
      updateAnalysisData(env.analysisResult[info.currentModule],
          info.nameToStore.split('.')[0], ['r']);
    }
    updateAnalysisData(env.analysisResult[info.currentModule],
        info.nameToStore, ['r']);
  }
};

// onWrite <~ is called before every write of an object
const onWrite = (info) => {
  if (info.parentName) {
    updateAnalysisData(env.analysisResult[info.currentModule],
        info.parentName, ['r']);
  }
  updateAnalysisData(env.analysisResult[info.currentModule],
      info.nameToStore, ['w']);
};

// onCallPre <~ is called before the execution of a function
const onCallPre = (info) => {
  if (info.typeClass === 'module-locals') {
    updateAnalysisData(env.analysisResult[info.currentModule],
        'require', ['r', 'x']);
    updateAnalysisData(env.analysisResult[info.currentModule],
        info.nameToStore, ['i']);
  } else {
    if (info.typeClass === 'node-globals') {
      updateAnalysisData(env.analysisResult[info.declareModule],
          info.nameToStore.split('.')[0], ['r']);
    }
    updateAnalysisData(env.analysisResult[info.declareModule],
        info.nameToStore, ['r', 'x']);
    if (pattern.test(info.nameToStore)) {
      updateAnalysisData(env.analysisResult[info.currentModule],
          info.nameToStore.match(pattern)[0], ['r']);
    }
  }
};

// onConstruct <~ Is call before every construct
const onConstruct = (info) => {
  updateAnalysisData(env.analysisResult[info.currentName],
      info.nameToStore, ['r', 'x']);
};

// onExit (toSave == place to save the result) --maybe make it module-local?
const onExit = (intersection, candidateModule) => {
  for (const name of intersection) {
    const currentName = candidateModule.get(name);
    updateAnalysisData(env.analysisResult[currentName],
        name, ['w']);
  }
  if (env.conf.reportTime) {
    const timerEnd = process.hrtime(env.conf.timerStart);
    const timeMillis = convert(timerEnd).millis;
    console.log(timeMillis, 'Time');
  }
  if (env.conf.print) {
    console.log(JSON.stringify(env.analysisResult, null, 2));
  }
};

module.exports = (e) => {
  env = e;
  return {
    onRead: onRead,
    onCallPre: onCallPre,
    onWrite: onWrite,
    onConstruct: onConstruct,
    onExit: onExit,
  };
};
