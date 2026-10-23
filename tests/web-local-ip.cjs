const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('web/tv.js', 'utf8');

function load(window) {
  const context = {window, setTimeout, clearTimeout, navigator: {language: 'en'}, console};
  vm.createContext(context);
  vm.runInContext(source, context);
  return context.TV;
}
function read(tv) {
  return new Promise(resolve => {
    if (typeof tv.localIp !== 'function') resolve('');
    else tv.localIp(resolve);
  });
}
function samsung(ip, connected = 1) {
  return load({tizen: {application: {}}, webapis: {network: {
    getActiveConnectionType: () => connected,
    getIp: () => ip
  }}});
}
function lg(result) {
  return load({PalmServiceBridge: function () {
    this.call = () => this.onservicecallback(JSON.stringify(result));
  }});
}
(async () => {
  assert.equal(await read(samsung('192.168.127.16')), '192.168.127.16');
  assert.equal(await read(samsung('192.168.127.16', 0)), '');
  for (const ip of ['127.0.0.1', '0.0.0.0', '169.254.1.2', '224.0.0.1', '256.1.1.1', '::1', '']) {
    assert.equal(await read(samsung(ip)), '', ip);
  }
  assert.equal(await read(load({tizen: {application: {}}, webapis: {network: {
    getActiveConnectionType() { throw Error('denied'); }
  }}})), '');
  assert.equal(await read(lg({returnValue: true, isInternetConnectionAvailable: false,
    wired: {state: 'connected', ipAddress: '10.0.0.8'}, wifi: {state: 'disconnected'}})), '10.0.0.8');
  assert.equal(await read(lg({returnValue: true, wired: {state: 'disconnected', ipAddress: '10.0.0.8'},
    wifi: {state: 'connected', ipAddress: '192.168.1.20'}})), '192.168.1.20');
  assert.equal(await read(lg({returnValue: false})), '');
  assert.equal(await read(load({})), '');
  console.log('Web local IP: 14 checks passed');
})().catch(error => { console.error(error); process.exitCode = 1; });
