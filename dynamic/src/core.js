const lya = require('@andromeda/lya');
const pathJoin = require('path').join;

lya.settings.context =  {
  enableWith: false,
  include: [
   'user-globals',
   'es-globals',
   'node-globals',
   'module-locals',
   'module-returns'],
  excludes: [],
};

lya.preset.RWX = (pathJoin(__dirname, './analyses/rwx.js'));
lya.preset.RWX_CHECKING = pathJoin(__dirname, './analyses/rwx-checking.js');
lya.preset.RWX_ENFORCEMENT = pathJoin(__dirname, './analyses/rwx-enforcement.js');
lya.preset.RWX_PERFORMANCE = pathJoin(__dirname, './analyses/rwx-performance.js');
lya.preset.STAR_CHECK = pathJoin(__dirname, './analyses/star-check.js');
  
module.exports = lya;

