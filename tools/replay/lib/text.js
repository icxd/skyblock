'use strict'
// Text components (as NBT) to legacy text with '&' colour codes, the way they look in game.
const nbt = require('prismarine-nbt')

const COLORS = {
  black: '0', dark_blue: '1', dark_green: '2', dark_aqua: '3', dark_red: '4', dark_purple: '5', gold: '6', gray: '7',
  dark_gray: '8', blue: '9', green: 'a', aqua: 'b', red: 'c', light_purple: 'd', yellow: 'e', white: 'f'
}
const DECORATIONS = [['bold', 'l'], ['italic', 'o'], ['underlined', 'n'], ['strikethrough', 'm'], ['obfuscated', 'k']]

function simplify (c) {
  return c && typeof c === 'object' && typeof c.type === 'string' && c.value !== undefined ? nbt.simplify(c) : c
}

/**
 * Legacy text for a component. Styles are inherited the way the client does it, including parts
 * that switch a decoration off; codes are only written where the style changes. Click actions
 * are shown as ⟦click:action value⟧.
 */
function text (component) {
  let last = ''
  const codes = style => {
    let s = style.color ? (style.color.startsWith('#') ? '&' + style.color : '&' + (COLORS[style.color] || '?')) : '&r'
    for (const [key, code] of DECORATIONS) if (style[key]) s += '&' + code
    if (s === last) return ''
    last = s
    return s
  }
  const walk = (c, inherited) => {
    c = simplify(c)
    if (c == null) return ''
    if (typeof c === 'string') return (c ? codes(inherited) : '') + c
    if (Array.isArray(c)) return c.map(x => walk(x, inherited)).join('')
    const style = { ...inherited }
    for (const key of ['color', ...DECORATIONS.map(d => d[0])]) if (c[key] !== undefined) style[key] = c[key]
    let s = ''
    const content = c.text ?? c['']
    if (content !== undefined && content !== '') s += codes(style) + content
    if (c.translate) s += codes(style) + '{' + c.translate + (c.with ? ':' + c.with.map(w => walk(w, style)).join('|') : '') + '}'
    if (c.extra) s += c.extra.map(e => walk(e, style)).join('')
    const click = c.clickEvent || c.click_event
    if (click) s += ' ⟦click:' + click.action + ' ' + (click.value || click.command || click.url || '') + '⟧'
    return s
  }
  // Servers also put § codes inside the text itself.
  return walk(component, {}).replace(/^&r/, '').replace(/§/g, '&')
}

/** Without any colour or formatting codes. */
function plain (s) {
  return s.replace(/[&§](#[0-9a-f]{6}|.)/gi, '')
}

module.exports = { text, plain, simplify }
