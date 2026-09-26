'use strict'
// `replay.js setup <server jar>`: tables minecraft-data doesn't have for newer versions, made
// from a vanilla server jar with Mojang's data generator. Nothing from the jar is committed.
//   data/items.json       numeric item id -> item name
//   data/packets.json     the generator's packet report (ids per state)
//   data/map-colors.json  ARGB for each packed map colour id
//   data/version.json     which version they're for
const fs = require('fs')
const os = require('os')
const path = require('path')
const { execFileSync } = require('child_process')

function findJars (dir) {
  const out = []
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name)
    if (e.isDirectory()) out.push(...findJars(p))
    else if (e.name.endsWith('.jar')) out.push(p)
  }
  return out
}

function setup (serverJar, dataDir, java) {
  const javac = java === 'java' ? 'javac' : path.join(path.dirname(java), process.platform === 'win32' ? 'javac.exe' : 'javac')
  const work = fs.mkdtempSync(path.join(os.tmpdir(), 'replay-setup-'))
  try {
    console.log('Running the data generator (a minute or so)...')
    // The server jar is a bundler: it unpacks the real server and its libraries into the working directory.
    execFileSync(java, ['-DbundlerMainClass=net.minecraft.data.Main', '-jar', path.resolve(serverJar), '--reports', '--output', 'generated'],
      { cwd: work, stdio: ['ignore', 'ignore', 'inherit'] })
    const reports = path.join(work, 'generated', 'reports')
    const version = fs.readdirSync(path.join(work, 'versions'))[0]

    fs.mkdirSync(dataDir, { recursive: true })
    const registries = JSON.parse(fs.readFileSync(path.join(reports, 'registries.json')))
    const items = {}
    for (const [name, e] of Object.entries(registries['minecraft:item'].entries)) items[e.protocol_id] = name.replace('minecraft:', '')
    fs.writeFileSync(path.join(dataDir, 'items.json'), JSON.stringify(items))
    fs.copyFileSync(path.join(reports, 'packets.json'), path.join(dataDir, 'packets.json'))

    console.log('Reading map colours...')
    const server = findJars(path.join(work, 'versions'))[0]
    const classpath = [server, ...findJars(path.join(work, 'libraries'))].join(path.delimiter)
    execFileSync(javac, ['-d', work, '-cp', classpath, path.join(__dirname, 'MapColors.java')], { stdio: 'inherit' })
    const colors = execFileSync(java, ['-cp', classpath + path.delimiter + work, 'MapColors'], { cwd: work, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] })
    fs.writeFileSync(path.join(dataDir, 'map-colors.json'), colors.trim())
    fs.writeFileSync(path.join(dataDir, 'version.json'), JSON.stringify({ version }))
    console.log(`Done: tables for ${version} in ${dataDir}`)
  } finally {
    fs.rmSync(work, { recursive: true, force: true })
  }
}

module.exports = { setup }
