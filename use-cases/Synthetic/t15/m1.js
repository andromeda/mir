var n = ['PWD', 'HOME', 'SHELL', 'PATH', 'CHAN'];

var s = '';
for (var i = 0; i < n.length; i++) {
  s += process.env[n[i]]
};
