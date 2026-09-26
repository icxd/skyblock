'use strict'
// Reading Replay Mod recordings: the packets the server sent, decoded with minecraft-data's
// layouts (patched where a newer version changed them, see PATCHES).
const fs = require('fs')
const path = require('path')
const AdmZip = require('adm-zip')
const minecraftData = require('minecraft-data')
const { createDeserializer } = require('minecraft-protocol/src/transforms/serializer')

// Versions minecraft-data doesn't know yet, by protocol number: which known version has the same
// packet ids, and how its layouts differ.
const PATCHES = {
  776: {
    version: '26.2',
    base: '26.1',
    notes: 'packet ids as 26.1; team parameters changed (from the 26.2 server jar)',
    apply (data) {
      // ClientboundSetPlayerTeamPacket.Parameters: display name, prefix, suffix, name tag
      // visibility, collision rule, optional colour, options byte.
      const params = ['container', [
        { name: 'name', type: 'anonymousNbt' },
        { name: 'prefix', type: 'anonymousNbt' },
        { name: 'suffix', type: 'anonymousNbt' },
        { name: 'nameTagVisibility', type: 'varint' },
        { name: 'collisionRule', type: 'varint' },
        { name: 'color', type: ['option', 'varint'] },
        { name: 'flags', type: 'i8' }
      ]]
      const fields = data.protocol.play.toClient.types.packet_teams[1][2].type[1].fields
      fields.add = params
      fields.change = params
    }
  }
}

/** The recording's metadata and raw packet stream, from a .mcpr or an unpacked recording.tmcpr. */
function open (file) {
  if (file.endsWith('.mcpr')) {
    const zip = new AdmZip(file)
    return { meta: JSON.parse(zip.readAsText('metaData.json')), packets: zip.readFile('recording.tmcpr') }
  }
  const metaFile = path.join(path.dirname(file), 'metaData.json')
  return { meta: fs.existsSync(metaFile) ? JSON.parse(fs.readFileSync(metaFile)) : {}, packets: fs.readFileSync(file) }
}

/** Decoders for the recording's version: { version, notes, data, byState }. */
function decoders (meta) {
  let version = meta.mcversion
  let notes = ''
  let data = version && minecraftData(version)
  if (!data || (meta.protocol && data.version.version !== meta.protocol)) {
    const patch = PATCHES[meta.protocol]
    if (patch) {
      version = patch.base
      data = minecraftData(version)
      patch.apply(data)
      notes = patch.notes
    } else {
      const known = minecraftData.supportedVersions.pc
      version = known[known.length - 1]
      data = minecraftData(version)
      notes = `protocol ${meta.protocol} is unknown; decoding as ${version}, which may be wrong`
    }
  }
  const make = state => createDeserializer({ state, isServer: false, version, noErrorLogging: true })
  return { version, notes, data, byState: { login: make('login'), configuration: make('configuration'), play: make('play') } }
}

/**
 * Every packet in order: { t (ms), state, name, params } or, when it couldn't be decoded,
 * { t, state, id, error }. `leftover` is set when a packet had bytes the layout didn't read.
 */
function * packets (recording, dec) {
  const buf = recording.packets
  // Replay Mod starts with the login success, then the configuration phase.
  let state = 'login'
  for (let off = 0; off + 8 <= buf.length;) {
    const t = buf.readInt32BE(off)
    const len = buf.readInt32BE(off + 4)
    const data = buf.subarray(off + 8, off + 8 + len)
    off += 8 + len
    let parsed
    try {
      parsed = dec.byState[state].parsePacketBuffer(data)
    } catch (e) {
      yield { t, state, id: data[0], error: e.message || String(e) }
      continue
    }
    const { name, params } = parsed.data
    yield { t, state, name, params, leftover: parsed.metadata && parsed.metadata.size !== data.length }
    if (state === 'login' && name === 'success') state = 'configuration'
    else if (state === 'configuration' && name === 'finish_configuration') state = 'play'
    else if (state === 'play' && name === 'start_configuration') state = 'configuration'
  }
}

module.exports = { open, decoders, packets, PATCHES }
