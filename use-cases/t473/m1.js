var interval = setInterval(function(str1, str2) {
  console.log(str1 + " " + str2);
}, 1, "Hello.", "How are you?");

clearInterval(interval);
