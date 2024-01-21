const fs = require('fs');
const subprocess = require('child_process');
const path = require('path');

const maps = fs.readdirSync(path.resolve('./maps/')).filter(p => p.startsWith('[test] ')).map(p => `"maps/${p}"`);

fs.renameSync(path.resolve('./matches'), path.resolve('./_matches'));
try {
    for (let map of maps) {
        subprocess.execSync(`gradlew run -pMaps=${map} TeamA=${process.argv[2]} TeamB=${process.argv[3]}`);
    }
} finally {
    fs.renameSync(path.resolve('./_matches'), path.resolve('./matches'));
}