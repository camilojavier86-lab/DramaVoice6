const DV6_SW_VERSION='1.1.1-10105';
const DV6_CACHE=`dramavoice6-v${DV6_SW_VERSION}`;

self.addEventListener('install',event=>{
  event.waitUntil((async()=>{
    const cache=await caches.open(DV6_CACHE);
    try{
      const response=await fetch('./index.html',{cache:'reload'});
      if(response.ok)await cache.put('./index.html',response.clone());
    }catch{}
    // Las futuras versiones quedan esperando hasta que el usuario pulse "Actualizar ahora".
  })());
});

self.addEventListener('activate',event=>{
  event.waitUntil((async()=>{
    const keys=await caches.keys();
    await Promise.all(keys.filter(k=>k.startsWith('dramavoice6-')&&k!==DV6_CACHE).map(k=>caches.delete(k)));
    await self.clients.claim();
    const clients=await self.clients.matchAll({type:'window',includeUncontrolled:true});
    clients.forEach(client=>client.postMessage({type:'DV6_SW_ACTIVATED',version:DV6_SW_VERSION}));
  })());
});

self.addEventListener('message',event=>{
  if(event.data?.type==='SKIP_WAITING')self.skipWaiting();
});

async function networkFirst(request){
  const cache=await caches.open(DV6_CACHE);
  try{
    const response=await fetch(request,{cache:'no-store'});
    if(response&&response.ok)await cache.put(request,response.clone());
    return response;
  }catch{
    return (await cache.match(request)) || (await cache.match('./index.html'));
  }
}

async function staleWhileRevalidate(request){
  const cache=await caches.open(DV6_CACHE);
  const cached=await cache.match(request);
  const network=fetch(request).then(response=>{
    if(response&&response.ok)cache.put(request,response.clone());
    return response;
  }).catch(()=>null);
  return cached || await network || Response.error();
}

self.addEventListener('fetch',event=>{
  const request=event.request;
  if(request.method!=='GET')return;
  const url=new URL(request.url);
  if(url.origin!==self.location.origin)return;

  if(url.pathname.endsWith('/version.json')||url.pathname.endsWith('version.json')){
    event.respondWith(fetch(new Request(request,{cache:'no-store'})));
    return;
  }
  if(request.mode==='navigate'||url.pathname.endsWith('/index.html')||url.pathname.endsWith('index.html')){
    event.respondWith(networkFirst(request));
    return;
  }
  event.respondWith(staleWhileRevalidate(request));
});
