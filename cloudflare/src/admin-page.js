export const ADMIN_HTML = `<!doctype html>
<html lang="ko">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Yamone 게임 운영</title>
  <style>
    :root{color-scheme:dark;--bg:#090e22;--panel:#151d35;--mint:#67e7db;--pink:#ff84af;--text:#f5f7ff;--muted:#9aa5c5}
    *{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:14px system-ui,sans-serif}
    main{max-width:1180px;margin:auto;padding:24px}.top{display:flex;gap:12px;align-items:center;flex-wrap:wrap}
    h1{font-size:26px;margin:0 auto 0 0}h2{font-size:18px;margin:0 0 14px}.panel{background:var(--panel);border-radius:18px;padding:18px;margin-top:16px}
    input,select,button{border:0;border-radius:10px;padding:11px 12px;background:#222d4b;color:var(--text)}
    button{cursor:pointer;font-weight:700}button.primary{background:var(--mint);color:var(--bg)}button.danger{background:#51243a;color:#ffc2d4}
    .cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:10px}.card{background:#202a47;border-radius:14px;padding:15px}.value{font-size:25px;font-weight:800;margin-top:6px}
    .muted{color:var(--muted)}.hidden{display:none!important}.scroll{overflow:auto}table{width:100%;border-collapse:collapse;min-width:820px}th,td{padding:11px 8px;border-bottom:1px solid #293552;text-align:left;white-space:nowrap}th{color:var(--muted)}
    .actions{display:flex;gap:6px}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:9px}.wide{grid-column:span 2}.error{color:#ff9dbd;margin-top:9px}.ok{color:var(--mint)}
    @media(max-width:620px){main{padding:14px}.wide{grid-column:span 1}}
  </style>
</head>
<body><main>
  <div class="top"><h1>YAMONE 게임 운영</h1><span id="state" class="muted">로그인 필요</span></div>
  <section id="login" class="panel">
    <h2>관리자 로그인</h2>
    <div class="top"><input id="token" type="password" placeholder="RANKING_ADMIN_SECRET" style="min-width:280px"><button class="primary" onclick="login()">열기</button></div>
    <div id="loginError" class="error"></div>
  </section>
  <div id="dashboard" class="hidden">
    <section class="panel top">
      <label>앱 <input id="appId" value="yamone_arcade2"></label>
      <label>통계 기간 <select id="days"><option>7</option><option selected>30</option><option>90</option><option value="3650">전체</option></select></label>
      <label>배치 <select id="sortMode"><option value="manual">수동</option><option value="popular_7d">최근 7일 인기순</option><option value="popular_all">전체 인기순</option></select></label>
      <button class="primary" onclick="reloadAll()">새로고침</button><button onclick="saveSortMode()">배치 방식 저장</button><button onclick="logout()">로그아웃</button>
    </section>
    <section class="panel"><h2>전체 통계</h2><div id="summary" class="cards"></div></section>
    <section class="panel"><h2>게임별 통계</h2><div class="scroll"><table><thead><tr><th>게임</th><th>시작</th><th>완료</th><th>사용자</th><th>완료율</th></tr></thead><tbody id="gameStats"></tbody></table></div></section>
    <section class="panel"><h2>국가별 통계</h2><div class="scroll"><table><thead><tr><th>국가</th><th>플레이</th><th>사용자</th></tr></thead><tbody id="countryStats"></tbody></table></div></section>
    <section class="panel"><h2>사용자별 게임 기록</h2><div class="scroll"><table><thead><tr><th>사용자</th><th>국가</th><th>게임</th><th>시작</th><th>완료</th><th>마지막 플레이</th></tr></thead><tbody id="playerStats"></tbody></table></div></section>
    <section class="panel">
      <div class="top"><h2>게임 노출·순서·랭킹 관리</h2><span class="muted">삭제는 통계를 보존하고 앱에서만 숨깁니다.</span></div>
      <div class="scroll"><table><thead><tr><th>순서</th><th>ID / 모드</th><th>이름</th><th>상태</th><th>플레이</th><th>랭킹 시즌</th><th>관리</th></tr></thead><tbody id="catalog"></tbody></table></div>
    </section>
    <section class="panel"><h2>게임 추가·수정</h2>
      <div class="grid">
        <input id="gameId" placeholder="game_id"><input id="modeId" value="normal" placeholder="mode_id">
        <input id="title" placeholder="게임 이름"><input id="scoreUnit" value="points" placeholder="scoreUnit">
        <input id="maxScore" type="number" value="100000" placeholder="최대 점수"><input id="displayOrder" type="number" value="100" placeholder="노출 순서">
        <label><input id="featured" type="checkbox"> 대표 게임</label><button class="primary" onclick="saveGame()">저장</button>
      </div><div id="saveMessage" class="muted"></div>
    </section>
    <section class="panel"><h2>최근 관리 이력</h2><div class="scroll"><table><thead><tr><th>시간</th><th>작업</th><th>대상</th><th>설명</th></tr></thead><tbody id="audit"></tbody></table></div></section>
  </div>
<script>
let secret=sessionStorage.getItem('yamoneAdminSecret')||'';let games=[];
const $=id=>document.getElementById(id);const esc=value=>String(value??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
async function api(path,options={}){const headers={'Content-Type':'application/json','Authorization':'Bearer '+secret,...(options.headers||{})};const res=await fetch(path,{...options,headers});const data=await res.json().catch(()=>({error:'INVALID_RESPONSE'}));if(!res.ok)throw new Error(data.error||('HTTP '+res.status));return data}
async function login(){secret=$('token').value.trim();$('loginError').textContent='';try{await api('/v1/admin/session');sessionStorage.setItem('yamoneAdminSecret',secret);$('login').classList.add('hidden');$('dashboard').classList.remove('hidden');$('state').textContent='연결됨';await reloadAll()}catch(e){$('loginError').textContent='관리자 키를 확인해주세요.';secret=''}}
function logout(){sessionStorage.removeItem('yamoneAdminSecret');location.reload()}
async function reloadAll(){try{const app=encodeURIComponent($('appId').value.trim());const days=encodeURIComponent($('days').value);const [stats,catalog]=await Promise.all([api('/v1/admin/stats?appId='+app+'&days='+days),api('/v1/admin/catalog?appId='+app)]);renderStats(stats);renderCatalog(catalog)}catch(e){$('state').textContent='오류: '+e.message}}
function renderStats(s){$('summary').innerHTML=[['전체 플레이',s.summary.plays],['완료',s.summary.completed],['사용자',s.summary.players],['게임',s.summary.games]].map(x=>'<div class="card"><div class="muted">'+x[0]+'</div><div class="value">'+Number(x[1]||0).toLocaleString()+'</div></div>').join('');$('gameStats').innerHTML=s.byGame.map(x=>'<tr><td>'+esc(x.title||x.gameId)+'</td><td>'+x.plays.toLocaleString()+'</td><td>'+x.completed.toLocaleString()+'</td><td>'+x.players.toLocaleString()+'</td><td>'+(x.plays?Math.round(x.completed*100/x.plays):0)+'%</td></tr>').join('');$('countryStats').innerHTML=s.byCountry.map(x=>'<tr><td>'+esc(x.countryCode||'미확인')+'</td><td>'+x.plays.toLocaleString()+'</td><td>'+x.players.toLocaleString()+'</td></tr>').join('');$('playerStats').innerHTML=s.byPlayerGame.map(x=>'<tr><td>'+esc(x.nickname||x.playerRef)+'</td><td>'+esc(x.countryCode||'미확인')+'</td><td>'+esc(x.title||x.gameId)+'</td><td>'+x.plays.toLocaleString()+'</td><td>'+x.completed.toLocaleString()+'</td><td>'+new Date(x.lastPlayedAt*1000).toLocaleString()+'</td></tr>').join('');$('audit').innerHTML=s.audit.map(x=>'<tr><td>'+new Date(x.createdAt*1000).toLocaleString()+'</td><td>'+esc(x.action)+'</td><td>'+esc([x.appId,x.gameId,x.modeId].filter(Boolean).join(' / '))+'</td><td>'+esc(x.detail)+'</td></tr>').join('')}
function renderCatalog(c){games=c.games;$('sortMode').value=c.sortMode;$('catalog').innerHTML=games.map((g,i)=>'<tr><td><div class="actions"><button onclick="moveGame('+i+',-1)">↑</button><button onclick="moveGame('+i+',1)">↓</button></div></td><td><b>'+esc(g.gameId)+'</b><br><span class="muted">'+esc(g.modeId)+'</span></td><td>'+esc(g.title)+(g.featured?' <span class="ok">★</span>':'')+'</td><td>'+(g.enabled&&g.status==='active'?'<span class="ok">노출</span>':'숨김')+'</td><td>'+Number(g.playCount||0).toLocaleString()+'</td><td>'+g.rankingEpoch+' / 앱 '+g.localResetEpoch+'</td><td><div class="actions"><button onclick="editGame('+i+')">수정</button><button onclick="toggleGame('+i+')">'+(g.enabled?'숨김':'노출')+'</button><button class="danger" onclick="resetRanking('+i+',false)">랭킹 초기화</button><button class="danger" onclick="resetRanking('+i+',true)">랭킹+앱기록</button></div></td></tr>').join('')}
async function saveSortMode(){await api('/v1/admin/apps/settings',{method:'PATCH',body:JSON.stringify({appId:$('appId').value.trim(),sortMode:$('sortMode').value})});await reloadAll()}
async function moveGame(index,delta){const next=index+delta;if(next<0||next>=games.length)return;[games[index],games[next]]=[games[next],games[index]];await api('/v1/admin/apps/order',{method:'PATCH',body:JSON.stringify({appId:$('appId').value.trim(),games:games.map(x=>({gameId:x.gameId,modeId:x.modeId}))})});await reloadAll()}
function editGame(i){const g=games[i];$('gameId').value=g.gameId;$('modeId').value=g.modeId;$('title').value=g.title;$('scoreUnit').value=g.scoreUnit;$('maxScore').value=g.maxScore;$('displayOrder').value=g.displayOrder;$('featured').checked=g.featured;scrollTo({top:document.body.scrollHeight,behavior:'smooth'})}
async function saveGame(){try{await api('/v1/admin/games',{method:'POST',body:JSON.stringify({appId:$('appId').value.trim(),gameId:$('gameId').value.trim(),modeId:$('modeId').value.trim(),title:$('title').value.trim(),scoreUnit:$('scoreUnit').value.trim(),maxScore:Number($('maxScore').value),displayOrder:Number($('displayOrder').value),featured:$('featured').checked,enabled:true})});$('saveMessage').textContent='저장했습니다. 현재 앱 코드에 없는 새 게임은 앱 업데이트 후 표시됩니다.';await reloadAll()}catch(e){$('saveMessage').textContent='저장 실패: '+e.message}}
async function toggleGame(i){const g=games[i];await api('/v1/admin/games/visibility',{method:'PATCH',body:JSON.stringify({appId:$('appId').value.trim(),gameId:g.gameId,modeId:g.modeId,enabled:!g.enabled})});await reloadAll()}
async function resetRanking(i,resetLocal){const g=games[i];const message=resetLocal?'온라인 랭킹과 다음 동기화 시 앱 내부 최고기록까지 초기화합니다. 계속할까요?':'온라인 랭킹만 초기화합니다. 계속할까요?';if(!confirm(g.title+'\n\n'+message))return;const reason=prompt('초기화 사유를 입력하세요.','테스트 기록 정리');if(reason===null)return;await api('/v1/admin/rankings/reset',{method:'POST',body:JSON.stringify({gameId:g.gameId,modeId:g.modeId,resetLocal,reason})});await reloadAll()}
if(secret){$('token').value=secret;login()}
</script></main></body></html>`;
