from itertools import product
import re
import subprocess
from time import gmtime, strftime, time

emojiMode = False
emojiMap = {
    'Won': ':heavy_check_mark:',
    'Lost': ':x:',
    'Tied': ':heavy_equals_sign:',
    'N/A': ':heavy_minus_sign:',
    'Error': ':heavy_exclamation_mark:'
}
errors = []

currentBot = 'SPAARK' #bot to test
#SPAARK IS THE BASELINE

# bots = ['A0', 'A1', 'B0', 'B1', 'C0', 'C1', 'D0', 'D1', 'E', 'F0', 'F1', 'G0', 'G1', 'H0', 'H1', 'I0', 'I1', 'J0', 'J1', 'K0', 'K1', 'L0', 'L1', 'M0', 'M1', 'N0', 'N1', 'AA', 'BB'] #Jan 28-29
# bots = ['A0', 'A1', 'A2', 'A3', 'B0', 'B1', 'B2', 'B3', 'C0', 'C1', 'C2', 'C3', 'D0', 'D1', 'D2', 'D3', 'E0', 'E1', 'E2', 'E3', 'F0', 'F1', 'F2', 'F3', 'G0', 'G1', 'G2', 'G3', 'H0', 'H1', 'H2', 'H3'] #Jan 27-28
bots = ['NOFILL']
#CHECK SCREENSHOTS FOR INFO ON WHICH BOT IS WHICH

maps = []

#Micro testing
# maps.append("[edge] microsmall")
# maps.append("[edge] micromedium")
# maps.append("[edge] microlarge")

#Default
# maps.append("DefaultSmall")
# maps.append("DefaultMedium")
# maps.append("DefaultLarge")
# maps.append("DefaultHuge")

#Sprint 1
# maps.append("AceOfSpades")
# maps.append("Alien")
# maps.append("Ambush")
# maps.append("Battlecode24")
# maps.append("BigDucksBigPond")
# maps.append("Canals")
# maps.append("CH3353C4K3F4CT0RY")
# maps.append("Duck")
# maps.append("Fountain")
# maps.append("Hockey")
# maps.append("HungerGames")
# maps.append("MazeRunner")
# maps.append("Rivers")
# maps.append("Snake")
# maps.append("Soccer")
# maps.append("SteamboatMickey")
# maps.append("Yinyang")

#Sprint 2
# maps.append("BedWars")
# maps.append("Bunkers")
# maps.append("Checkered")
# maps.append("Diagonal")
# maps.append("Divergent")
# maps.append("EndAround")
# maps.append("FloodGates")
# maps.append("Foxes")
# maps.append("Fusbol")
# maps.append("GaltonBoard")
# maps.append("HeMustBeFreed")
# maps.append("Intercontinental")
# maps.append("Klein")
# maps.append("QueenOfHearts")
# maps.append("QuestionableChess")
# maps.append("Racetrack")
# maps.append("Rainbow")
# maps.append("TreeSearch")

#Water
maps.append("Racetrack")
maps.append("BedWars")
maps.append("DefaultLarge")
maps.append("BigDucksBigPond")
maps.append("Fountain")
maps.append("Canals")
maps.append("QuestionableChess")
maps.append("Fusbol")

matches = list(product(bots, maps))

numWinsMapping = {
    0: 'Lost',
    1: 'Tied',
    2: 'Won',
}

def retrieveGameLength(output):
    startIndex = output.find('wins (round ')
    if startIndex == -1:
        return -1
    endIndex = output.find(')', startIndex)
    if endIndex == -1:
        return -1
    return output[startIndex + len('wins(round ') + 1:endIndex]

def run_match(bot, map):
    try:
        outputA = str(subprocess.check_output(['./gradlew', 'run', '-PteamA=' + currentBot, '-PteamB=' + bot, '-Pmaps=' + map]))
        outputB = str(subprocess.check_output(['./gradlew', 'run', '-PteamA=' + bot, '-PteamB=' + currentBot, '-Pmaps=' + map]))
        # for local windows testing
        #outputA = str(subprocess.check_output(['gradlew', 'run', '-PteamA=' + currentBot, '-PteamB=' + bot, '-Pmaps=' + map], shell=True))
        #outputB = str(subprocess.check_output(['gradlew', 'run', '-PteamA=' + bot, '-PteamB=' + currentBot, '-Pmaps=' + map], shell=True))
    except subprocess.CalledProcessError as exc:
        print("Status: FAIL", exc.returncode, exc.output)
        return 'Error'
    else:
        winAString = '{} (A) wins'.format(currentBot)
        winBString = '{} (B) wins'.format(currentBot)
        loseAString = '{} (B) wins'.format(bot)
        loseBString = '{} (A) wins'.format(bot)
        resignedString = 'resigned'
        
        numWins = 0
        
        gameLengthA = retrieveGameLength(outputA)
        gameAResigned = resignedString in outputA
        gameLengthB = retrieveGameLength(outputB)
        gameBResigned = resignedString in outputB

        flagRegex = "FLAG{[^{}]*}"
        gameAFlags = list(set(re.findall(flagRegex, outputA)))
        gameBFlags = list(set(re.findall(flagRegex, outputB)))
        gameAFlags = ", ".join([s[5:-1] for s in gameAFlags])
        gameBFlags = ", ".join([s[5:-1] for s in gameBFlags])
        if len(gameAFlags) > 0:
            gameAFlags = "{{{}}}".format(gameAFlags)
        if len(gameBFlags) > 0:
            gameBFlags = "{{{}}}".format(gameBFlags)

        gameAInfo = gameLengthA + ('*' if gameAResigned else '') + gameAFlags
        gameBInfo = gameLengthB + ('*' if gameBResigned else '') + gameBFlags
        
        if winAString in outputA:
            numWins += 1
        else:
            if not loseAString in outputA:
                return 'Error'
        if winBString in outputB:
            numWins += 1
        else:
            if not loseBString in outputB:
                return 'Error'
        return (numWinsMapping[numWins] + ' (' + ', '.join([gameAInfo, gameBInfo]) + ')', numWins)

results = {}
ctr = 0
#run matches
currentTime = time()
for i in range(len(bots)):
    bot = bots[i]
    winsThisBot = 0
    for j in range(len(maps)):
        map = maps[j]
        ctr = ctr + 1
        print("(" + strftime("%H:%M:%S", gmtime()) + ") " + str(ctr) + " of " + str(len(bots)*len(maps)) + ": {} vs {} on {}".format(currentBot, bot, map), end=" ", flush=True)
        results[(bot, map)], wins = run_match(bot, map)
        winsThisBot += wins
        print(currentBot + " won " + str(round(winsThisBot / (j*2 + 2) * 100)) + "% against " + bot)
    print(currentBot + " won " + str(winsThisBot) + " of " + str(len(maps)*2) + " against " + bot + "\n")

# Construct table
table = [[results.get((bot, map), 'N/A') for bot in bots] for map in maps]

def replaceWithDictionary(s, mapping):
    for a, b in mapping.items():
        s = s.replace(a, b)
    return s

if emojiMode:
    table = [[replaceWithDictionary(item, emojiMap) for item in row] for row in table]

# Write to file
with open('matches-summary.txt', 'w') as f:
    table = [[''] + bots, [':---:' for i in range(len(bots) + 1)]] + [[map] + row for map, row in zip(maps, table)]
    for line in table:
        f.write('| ')
        f.write(' | '.join(line))
        f.write(' |')
        f.write('\n')
    f.write('\n')
    for error in errors:
        f.write(error)

print("Took " + str((time() - currentTime) / 60) + " minutes")