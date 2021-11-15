var _;
_ = Promise.length;
_ = Promise.name;
_ = Promise.prototype;
_ = Promise.all;
_ = Promise.race;
_ = Promise.resolve;
_ = Promise.reject;
new Promise((resolve, reject) => {
  // We call resolve(...) when what we were doing asynchronously was successful, and reject(...) when it failed.
  // In this example, we use setTimeout(...) to simulate async code.
  // In reality, you will probably be using something like XHR or an HTML5 API.
  setTimeout( function() {
    resolve("Success!")  // Yay! Everything went well!
  }, 250)
})
Promise.all();
Promise.race();
Promise.resolve();
Promise.reject();
