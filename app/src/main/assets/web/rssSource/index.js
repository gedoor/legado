// 简化js原生选择器
function $(selector) { return document.querySelector(selector); }
function $$(selector) { return document.querySelectorAll(selector); }
// 读写Hash值(val未赋值时为读取)
function hashParam(key, val) {
	let hashstr = decodeURIComponent(window.location.hash);
	let regKey = new RegExp(`${key}=([^&]*)`);
	let getVal = regKey.test(hashstr) ? hashstr.match(regKey)[1] : null;
	if (val == undefined) return getVal;
	if (hashstr == '' || hashstr == '#') {
		window.location.hash = `#${key}=${val}`;
	}
	else {
		if (getVal) window.location.hash = hashstr.replace(getVal, val);
		else {
			window.location.hash = hashstr.indexOf(key) > -1 ? hashstr.replace(regKey, `${key}=${val}`) : `${hashstr}&${key}=${val}`;
		}
	}
}
// 创建源规则容器对象
function Container() {
	let ruleJson = {};

	// 基本以及其他
	$$('.rules .base').forEach(item => ruleJson[item.title] = '');
	ruleJson.customOrder = 0;
	ruleJson.enabled = true;

	return ruleJson;
}
// 选项卡Tab切换事件处理
function showTab(tabName) {
	$$('.tabtitle>*').forEach(node => { node.className = node.className.replace(' this', ''); });
	$$('.tabbody>*').forEach(node => { node.className = node.className.replace(' this', ''); });
	$(`.tabbody>.${$(`.tabtitle>*[name=${tabName}]`).className}`).className += ' this';
	$(`.tabtitle>*[name=${tabName}]`).className += ' this';
	hashParam('tab', tabName);
}
// 源列表列表标签构造函数
function newRule(rule) {
	return `<label for="${hex_md5(rule.sourceUrl)}"><input type="radio" name="rule" id="${hex_md5(rule.sourceUrl)}"><div>${rule.sourceName}<br>${rule.sourceUrl}</div></label>`;
}
// 缓存规则列表
var RuleSources = [];
if (localStorage.getItem('RssSources')) {
	RuleSources = JSON.parse(localStorage.getItem('RssSources'));
	RuleSources.forEach(item => $('#RuleList').innerHTML += newRule(item));
}
// 页面加载完成事件
window.onload = () => {
	$$('.tabtitle>*').forEach(item => {
		item.addEventListener('click', () => {
			showTab(item.innerHTML);
		});
	});
	if (hashParam('tab')) showTab(hashParam('tab'));
}
// 获取数据
function HttpGet(url) {
	return fetch(hashParam('domain') ? hashParam('domain') + url : url)
		.then(res => res.json()).catch(err => console.error('Error:', err));
}
// 提交数据
function HttpPost(url, data) {
	return fetch(hashParam('domain') ? hashParam('domain') + url : url, {
		body: JSON.stringify(data),
		method: 'POST',
		mode: "cors",
		headers: new Headers({
			'Content-Type': 'application/json;charset=utf-8'
		})
	}).then(res => res.json()).catch(err => console.error('Error:', err));
}
// 将源表单转化为源对象
function rule2json() {
	let RuleJSON = Container();
	// 转换base
	Object.keys(RuleJSON).forEach(key => {
		if (!key.startsWith("rule")) {
			RuleJSON[key] = $('#' + key).value;
		}
	});

	RuleJSON.lastUpdateTime = new Date().getTime();
	RuleJSON.customOrder = RuleJSON.customOrder == '' ? 0 : parseInt(RuleJSON.customOrder);
	RuleJSON.enabled = RuleJSON.enabled == '' || String(RuleJSON.enabled).toLocaleLowerCase().replace(/^\s*|\s*$/g, '') == 'true';
	return RuleJSON;
}
// 将源对象填充到源表单
function json2rule(RuleEditor) {
	let RuleJSON = Container();
	// 转换base
	Object.keys(RuleJSON).forEach(key => {
		let val = RuleEditor[key];
        if (typeof val == "number") {
            $("#" + key).value = val ? String(val) : '0';
        }
        else if (typeof val == "boolean") {
            $("#" + key).value = val ? String(val) : 'false';
        }
        else {
            $("#" + key).value = val ? String(val) : '';
        }
	});
}
// 记录操作过程
var course = { "old": [], "now": {}, "new": [] };
if (localStorage.getItem('rssSourceCourse')) {
	course = JSON.parse(localStorage.getItem('rssSourceCourse'));
	json2rule(course.now);
}
else {
	course.now = rule2json();
	window.localStorage.setItem('rssSourceCourse', JSON.stringify(course));
}
function todo() {
	course.old.push(Object.assign({}, course.now));
	course.now = rule2json();
	course.new = [];
	if (course.old.length > 50) course.old.shift(); // 限制历史记录堆栈大小
	localStorage.setItem('rssSourceCourse', JSON.stringify(course));
}
function undo() {
	course = JSON.parse(localStorage.getItem('rssSourceCourse'));
	if (course.old.length > 0) {
		course.new.push(course.now);
		course.now = course.old.pop();
		localStorage.setItem('rssSourceCourse', JSON.stringify(course));
		json2rule(course.now);
	}
}
function redo() {
	course = JSON.parse(localStorage.getItem('rssSourceCourse'));
	if (course.new.length > 0) {
		course.old.push(course.now);
		course.now = course.new.pop();
		localStorage.setItem('rssSourceCourse', JSON.stringify(course));
		json2rule(course.now);
	}
}
function setRule(editRule) {
	let checkRule = RuleSources.find(x => x.sourceUrl == editRule.sourceUrl);
	if ($(`input[id="${editRule.sourceUrl}"]`)) {
		Object.keys(checkRule).forEach(key => { checkRule[key] = editRule[key]; });
		$(`input[id="${editRule.sourceUrl}"]+*`).innerHTML = `${editRule.sourceName}<br>${editRule.sourceUrl}`;
	} else {
		RuleSources.push(editRule);
		$('#RuleList').innerHTML += newRule(editRule);
	}
}
$$('input').forEach((item) => { item.addEventListener('change', () => { todo() }) });
$$('textarea').forEach((item) => { item.addEventListener('change', () => { todo() }) });
// 处理按钮点击事件
$('.menu').addEventListener('click', e => {
	let thisNode = e.target;
	thisNode = thisNode.parentNode.nodeName == 'svg' ? thisNode.parentNode.querySelector('rect') :
		thisNode.nodeName == 'svg' ? thisNode.querySelector('rect') : null;
	if (!thisNode) return;
	if (thisNode.getAttribute('class') == 'busy') return;
	thisNode.setAttribute('class', 'busy');
	switch (thisNode.id) {
		case 'push':
			$$('#RuleList>label>div').forEach(item => { item.className = ''; });
			(async () => {
				await HttpPost(`/saveRssSources`, RuleSources).then(json => {
					if (json.isSuccess) {
						let okData = json.data;
						if (Array.isArray(okData)) {
							let failMsg = ``;
							if (RuleSources.length > okData.length) {
								RuleSources.forEach(item => {
									if (okData.find(x => x.sourceUrl == item.sourceUrl)) { }
									else { $(`#RuleList #${item.sourceUrl}+*`).className += 'isError'; }
								});
								failMsg = '\n推送失败的源将用红色字体标注!';
							}
							alert(`批量推送源到「阅读3.0APP」\n共计: ${RuleSources.length} 条\n成功: ${okData.length} 条\n失败: ${RuleSources.length - okData.length} 条${failMsg}`);
						}
						else {
							alert(`批量推送源到「阅读3.0APP」成功!\n共计: ${RuleSources.length} 条`);
						}
					}
					else {
						alert(`批量推送源失败!\nErrorMsg: ${json.errorMsg}`);
					}
				}).catch(err => { alert(`批量推送源失败,无法连接到「阅读3.0APP」!\n${err}`); });
				thisNode.setAttribute('class', '');
			})();
			return;
		case 'pull':
			showTab('源列表');
			(async () => {
				await HttpGet(`/getRssSources`).then(json => {
					if (json.isSuccess) {
						$('#RuleList').innerHTML = ''
						localStorage.setItem('RssSources', JSON.stringify(RuleSources = json.data));
						RuleSources.forEach(item => {
							$('#RuleList').innerHTML += newRule(item);
						});
						alert(`成功拉取 ${RuleSources.length} 条源`);
					}
					else {
						alert(`批量拉取源失败!\nErrorMsg: ${json.errorMsg}`);
					}
				}).catch(err => { alert(`批量拉取源失败,无法连接到「阅读3.0APP」!\n${err}`); });
				thisNode.setAttribute('class', '');
			})();
			return;
		case 'editor':
			if ($('#RuleJsonString').value == '') break;
			try {
				json2rule(JSON.parse($('#RuleJsonString').value));
				todo();
			} catch (error) {
				console.log(error);
				alert(error);
			}
			break;
		case 'conver':
			showTab('编辑源');
			$('#RuleJsonString').value = JSON.stringify(rule2json(), null, 4);
			break;
		case 'initial':
			$$('.rules textarea').forEach(item => { item.value = '' });
			todo();
			break;
		case 'undo':
			undo()
			break;
		case 'redo':
			redo()
			break;
		case 'debug':
			showTab('调试源');
			let wsOrigin = (hashParam('domain') || location.origin).replace(/^.*?:/, 'ws:').replace(/\d+$/, (port) => (parseInt(port) + 1));
			let DebugInfos = $('#DebugConsole');
			function DebugPrint(msg) { DebugInfos.value += `\n${msg}`; DebugInfos.scrollTop = DebugInfos.scrollHeight; }
			let saveRule = [rule2json()];
			HttpPost(`/saveRssSources`, saveRule).then(sResult => {
				if (sResult.isSuccess) {
					let sKey = DebugKey.value ? DebugKey.value : '我的';
					$('#DebugConsole').value = `源《${saveRule[0].sourceName}》保存成功！使用搜索关键字“${sKey}”开始调试...`;
					let ws = new WebSocket(`${wsOrigin}/sourceDebug`);
					ws.onopen = () => {
						ws.send(`{"tag":"${saveRule[0].sourceUrl}", "key":"${sKey}"}`);
					};
					ws.onmessage = (msg) => {
					    console.log('[调试]', msg);
						DebugPrint(msg.data);
					};
					ws.onerror = (err) => {
						throw `${err.data}`;
					}
					ws.onclose = () => {
						thisNode.setAttribute('class', '');
						DebugPrint(`\n调试服务已关闭!`);
					}
				} else throw `${sResult.errorMsg}`;
			}).catch(err => {
				DebugPrint(`调试过程意外中止，以下是详细错误信息:\n${err}`);
				thisNode.setAttribute('class', '');
			});
			return;
		case 'accept':
			(async () => {
				let saveRule = [rule2json()];
				await HttpPost(`/saveRssSources`, saveRule).then(json => {
					alert(json.isSuccess ? `源《${saveRule[0].sourceName}》已成功保存到「阅读3.0APP」` : `源《${saveRule[0].sourceName}》保存失败!\nErrorMsg: ${json.errorMsg}`);
					setRule(saveRule[0]);
				}).catch(err => { alert(`保存源失败,无法连接到「阅读3.0APP」!\n${err}`); });
				thisNode.setAttribute('class', '');
			})();
			return;
		default:
	}
	setTimeout(() => { thisNode.setAttribute('class', ''); }, 500);
});
$('#Filter').addEventListener('keydown', e => {
	if (e.keyCode == 13) {
		let cashList = [];
		$('#RuleList').innerHTML = "";
		let sKey = Filter.value ? Filter.value : '';
		if (sKey == '') {
			cashList = RuleSources;
		} else {
			let patt = new RegExp(sKey);
			RuleSources.forEach(source => {
				if (patt.test(source.sourceUrl) || patt.test(source.sourceName) || patt.test(source.bookSourceGroup)) {
					cashList.push(source);
				}
			})
		}
		cashList.forEach(source => {
			$('#RuleList').innerHTML += newRule(source);
		})
	}
});

// 列表规则更改事件
$('#RuleList').addEventListener('click', e => {
	let editRule = null;
	if (e.target && e.target.getAttribute('name') == 'rule') {
		editRule = rule2json();
		json2rule(RuleSources.find(x => hex_md5(x.sourceUrl) == e.target.id));
	} else return;
	if (editRule.sourceUrl == '') return;
	if (editRule.sourceName == '') editRule.sourceName = editRule.sourceUrl.replace(/.*?\/\/|\/.*/g, '');
	setRule(editRule);
	localStorage.setItem('RssSources', JSON.stringify(RuleSources));
});
// 处理列表按钮事件
$('.tab3>.titlebar').addEventListener('click', e => {
	let thisNode = e.target;
	if (thisNode.nodeName != 'BUTTON') return;
	switch (thisNode.id) {
		case 'Import':
			let fileImport = document.createElement('input');
			fileImport.type = 'file';
			fileImport.accept = '.json';
			fileImport.addEventListener('change', () => {
				let file = fileImport.files[0];
				let reader = new FileReader();
				reader.onloadend = function (evt) {
					if (evt.target.readyState == FileReader.DONE) {
						let fileText = evt.target.result;
						try {
							let fileJson = JSON.parse(fileText);
							let newSources = [];
							newSources.push(...fileJson);
							if (window.confirm(`如何处理导入的源?\n"确定": 覆盖当前列表(不会删除APP源)\n"取消": 插入列表尾部(自动忽略重复源)`)) {
								localStorage.setItem('RssSources', JSON.stringify(RuleSources = newSources));
								$('#RuleList').innerHTML = ''
								RuleSources.forEach(item => {
									$('#RuleList').innerHTML += newRule(item);
								});
							}
							else {
								newSources = newSources.filter(item => !JSON.stringify(RuleSources).includes(item.sourceUrl));
								RuleSources.push(...newSources);
								localStorage.setItem('RssSources', JSON.stringify(RuleSources));
								newSources.forEach(item => {
									$('#RuleList').innerHTML += newRule(item);
								});
							}
							alert(`成功导入 ${newSources.length} 条源`);
						}
						catch (err) {
							alert(`导入源文件失败!\n${err}`);
						}
					}
				};
				reader.readAsText(file);
			}, false);
			fileImport.click();
			break;
		case 'Export':
			let fileExport = document.createElement('a');
			fileExport.download = `Rules${Date().replace(/.*?\s(\d+)\s(\d+)\s(\d+:\d+:\d+).*/, '$2$1$3').replace(/:/g, '')}.json`;
			let myBlob = new Blob([JSON.stringify(RuleSources, null, 4)], { type: "application/json" });
			fileExport.href = window.URL.createObjectURL(myBlob);
			fileExport.click();
			break;
		case 'Delete':
			let selectRule = $('#RuleList input:checked');
			if (!selectRule) {
				alert(`没有源被选中!`);
				return;
			}
			if (confirm(`确定要删除选定源吗?\n(同时删除APP内源)`)) {
				let selectRuleUrl = selectRule.id;
				let deleteSources = RuleSources.filter(item => item.sourceUrl == selectRuleUrl); // 提取待删除的源
				let laveSources = RuleSources.filter(item => !(item.sourceUrl == selectRuleUrl));  // 提取待留下的源
				HttpPost(`/deleteRssSources`, deleteSources).then(json => {
					if (json.isSuccess) {
						let selectNode = document.getElementById(selectRuleUrl).parentNode;
						selectNode.parentNode.removeChild(selectNode);
						localStorage.setItem('RssSources', JSON.stringify(RuleSources = laveSources));
						if ($('#sourceUrl').value == selectRuleUrl) {
							$$('.rules textarea').forEach(item => { item.value = '' });
							todo();
						}
						console.log(deleteSources);
						console.log(`以上源已删除!`)
					}
				}).catch(err => { alert(`删除源失败,无法连接到「阅读3.0APP」!\n${err}`); });
			}
			break;
		case 'ClrAll':
			if (confirm(`确定要清空当前源列表吗?\n(不会删除APP内源)`)) {
				localStorage.setItem('RssSources', JSON.stringify(RuleSources = []));
				$('#RuleList').innerHTML = ''
			}
			break;
		default:
	}
});
