const fs = require('fs');
const subprocess = require('child_process');
const path = require('path');

const maps = fs.readdirSync(path.resolve('./maps/')).filter(p => p.startsWith('[test] ')).map(p => `"${p}"`);

let stop = () => {
    fs.renameSync(path.resolve('./_matches/'), path.resolve('./matches/'));
};

if (fs.existsSync(path.resolve('./matches/'))) fs.renameSync(path.resolve('./matches/'), path.resolve('./_matches/'));
try {
    for (let map of maps) {
        try {
            console.log(map)
            let result = subprocess.execSync(`gradlew run -Pmaps=${map} -PteamA=src/${process.argv[2]} -PteamB=src/${process.argv[3]} --debug`);
            console.log(result)
        } catch (err) {
            // idk error handling
        }
    }
} finally {
    stop();
}
process.on('SIGINT', stop);
process.on('SIGILL', stop);
process.on('SIGKILL', stop);
process.on('SIGBREAK', stop);