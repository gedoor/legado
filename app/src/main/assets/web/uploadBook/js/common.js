/**
 * 公共函数
 */

//全局的配置文件 
var config = {
	fileTypes: "txt|epub|umd", //允许上传的文件格式 "txt|epub" // |doc|docx|wps|xls|xlsx|et|ppt|pptx|dps
	//url : "http://"+location.host+"?action=addBook",//"http://localhost/t/post.php",//
	url: "../addLocalBook",
	fileLimitSize: 500 * 1024 * 1024

}

var file = {
	"inQueue": [], //已经在队列里面的文件，包括 HTML5上传和 Flash上传的
	"clientHaveFiles": [] // 客户端已经存在的文件列表
}

/**
 * HTML5 和 flash 公用，所有文件对象集合
 * @var array 
 */
var filesUpload = []; //

$.ajax({
	url: "http://" + location.host + '?action=getBooksList&t=' + (+new Date()),//"http://localhost/t/t.php",//
	async: false,//同步获取数据
	dataType: "json",
	success: function (data) {

		try {
			var dataLen = data.length;
			if (dataLen > 0) {
				for (var i = 0; i < dataLen; i++) {
					filesUpload.push(data[i]);
				}
			}
		} catch (e) { }

	}
})

//统计文件大小
function countFileSize(fileSize) {
	var KB = 1024;
	var MB = 1024 * 1024;
	if (KB >= fileSize) {
		return fileSize + "B";
	} else if (MB >= fileSize) {
		return (fileSize / KB).toFixed(2) + "KB";
	} else {
		return (fileSize / MB).toFixed(2) + "MB";
	}
}

//如果文件太长进行截取
function substr_string(name) {
	var maxLen = 15;
	var len = name.length;
	if (len < 17) return name;

	var lastIndex = name.lastIndexOf(".");
	var suffix = name.substr(lastIndex);
	var pre = name.substr(0, lastIndex);
	var preLen = pre.length;
	var preStart = preLen - 10;
	//前面10个 + 后面5个
	var fileName = pre.substr(0, 10) + "...." + pre.substr(preStart > 4 ? -4 : -preStart, 4) + suffix;
	return fileName
}


function checkFile(file) {

	if (file.size > config.fileLimitSize) {
		return jsonLang.t11;
	}

	if (!file.name || !file.name.toLowerCase().match('(' + config.fileTypes + ')$')) {
		return jsonLang.t12;
	}

	var len = filesUpload.length;
	for (var i = 0; i < len; i++) {
		if (filesUpload[i].name == file.name) {
			return jsonLang.t13;
		}
	}
	return null;
}

/**
 * 添加文件时，回调的函数
 * @param object file 文件对象
 * @param int type 0 是swf 上传的，1 是html5上传的
 */
function fileQueued(file, type) {
	var size = 0, fid = file.id, name = "";
	type = type || 0;

	if (file != undefined) {
		//计算文件大小 单位MB
		size = countFileSize(file.size);
		name = substr_string(file.name)
		//创建要插入的元素
		//		"<tr id='tr_'"+fid+"><td><div class='bh-poStion'><h1>"+name+"</h1>"+
		//						"<div class='bh-tip bh-tip3' id='progress_bar_"+fid+"'>"+
		//						"<span id='progress_bar_span_"+fid+"'></span></div>"+
		//						"</div></td><td><span class='bh-M'>"+size+"</span></td><td><div class='bh-link' id='handle_button_"+fid+"'>"+
		//						"<a href='javascript:void(0)' onclick=userCancelUpload('"+fid+"',"+type+")>取消</a></div></td></tr>";

		var HTML = '<li  id="tr_"' + fid + '>' +
			'<dl class="grybg">' +
			'<dt>' + name + '</dt>' +
			'<dd>' + size + '</dd>' +
			'<dd id="handle_button_' + fid + '"  onclick=userCancelUpload("' + fid + '",' + type + ') class="orange">' +
			'<span id="progress_bar_span_' + fid + '">0%</span> ' + jsonLang.t9 + '</dd>' +
			'</dl>' +
			'<div class="jdt"><p  id="progress_bar_p_' + fid + '" ></p></div>' +
			'</li>';


		jQuery("#tableStyle").append(HTML);
		//保存falsh_id，为上传做准备
		//global_flash_id.push(file.id);
		//更改背景颜色
		changeTrBackGroundColor()
	}
}

function changeTrBackGroundColor() {
	var getTr = document.getElementById("tableStyle").getElementsByTagName("dl");
	trNum = getTr.length;
	for (var i = 0; i < trNum; i++) {
		if (i % 2 == 0) {
			getTr[i].style.backgroundColor = "#f3f3f3";
		}
	}
}



//上传时返回的状态
function uploadProgress(file, bytesLoaded, bytesTotal) {
	if (!$("#progress_bar_p_" + file.id).hasClass("orange")) {
		$("#progress_bar_p_" + file.id).addClass("orange");
	}
	$("#progress_bar_p_" + file.id).css("width", (bytesLoaded / bytesTotal) * 100 + "%");
	$("#progress_bar_span_" + file.id).html(parseInt((bytesLoaded / bytesTotal) * 100) + "%");

}


//上传成功
function uploadSuccess(file, serverData, res) {
	var id = "handle_button_" + file.id;
	$("#" + id).replaceWith("<dd>" + jsonLang.t10 + "</dd>")
}


//取消上传
function userCancelUpload(file_id, type) {

	if (type == 0) {
		SWFFuns.cancelUpload(file_id);
	} else {
		HTML5Funs.cancelUpload(file_id);
	}

	$("#handle_button_" + file_id).html(jsonLang.t14).removeClass("orange").addClass("gray");
	//如果已经上传一部分了
	if ($("#progress_bar_p_" + file_id).hasClass("orange")) {
		$("#progress_bar_p_" + file_id).removeClass("orange");
		$("#progress_bar_p_" + file_id).addClass("gray");
	}
}


/**
 * 通过文件名称 从全局的文件列表中获取文件对象
 *
 */
function getFileFomeFilesUpload(filename) {
	var len = filesUpload.length;
	for (var i = 0; i < len; i++) {
		if (filesUpload[i].name == filename) {
			return filesUpload[i];
		}
	}

	return null;
}


/**
 * 往全局的 上传列表添加一个数据
 */
function addFileToFilesUpload(file) {

	if (typeof file == "string") {
		filesUpload.push({ "name": file })
		return true;
	} else if (typeof file == "object") {
		filesUpload.push(file);
		return true;
	}

	return false;
}

/**
 * 往全局的 上传列表添加一个数据
 */
function updateFileToFilesUpload(file) {

	var len = filesUpload.length;
	for (var i = 0; i < len; i++) {
		if (filesUpload[i].name == file.name) {
			filesUpload[i] = file;
			return true;
		}
	}

	return false;
}

/**
 * 查找在数组中的位置
 */
function findObjectKey(object, fid) {
	var len = object.length;
	for (var i = 0; i < len; i++) {
		if (object[i].id == fid) {
			return i;
		}
	}
	return -1;
}

/**
 * 从全局的文件集合中移除文件，一般上传失败时使用
 * @param array files   文件对象集合  [{},{},{}]
 * @param int fid  要删除的文件id
 * @return 删除后的数组，  其实数组是引用类型可以不返回
 */
function removeFileFromFilesUpload(files, fid) {

	var filesUploadKey = -1;

	filesUploadKey = findObjectKey(files, fid);
	//从全局文件中移除
	if (filesUploadKey > -1)
		files.splice(filesUploadKey, 1);

	return files;
}