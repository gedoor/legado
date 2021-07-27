/**
 * 处理拖拽上传
 */
var isDragOver = false;//拖拽触发点
var fileNumber = -1; //上传文件编号
var fileNumberPex = "zyFileUpload_"; //编号前缀
var currUploadfile = {}; //当前上传的文件对象

var uploadQueue = [];//上传队列集合
var isUploading = false;//是否正在上传

var XHR
try {
	XHR = new XMLHttpRequest();
} catch (e) { }

(function (isSupportFileUpload) {

	//不支持拖拽上传，或者 不支持FormData ，显示WiFi表示
	if (!isSupportFileUpload) {
		$("#drag_area").addClass("wf_wifi");
		$("#drag_area h3").html("");
		return;
		//更换样式
	} else {
		$("#drag_area").addClass("wf_normal");
	}


	addEvent();

	/**
	 * 添加事件
	 */
	function addEvent() {
		var dropArea = $('#drag_area h3')[0];
		dropArea.addEventListener('dragover', handleDragOver, false);
		dropArea.addEventListener('dragleave', handleDragLeave, false);
		dropArea.addEventListener('drop', handleDrop, false);
	}

	/**
	 * 松开拖拽文件的处理，进行上传
	 */
	function handleDrop(evt) {

		evt.stopPropagation();
		evt.preventDefault();
		$('#drag_area').addClass('wf_normal').removeClass('wf_active');

		console.log("Drop");
		isDragOver = false;

		var file = {};
		var errorMsgs = [];
		var len = evt.dataTransfer.files.length;

		for (var i = 0; i < len; i++) {
			fileNumber++;
			file = evt.dataTransfer.files[i];
			file.id = fileNumberPex + fileNumber;

			//检测文件
			msg = checkFile(file)
			//文件可以通过
			if (!msg) {
				//添加全局
				filesUpload.push(file);
				//添加上传队列
				uploadQueue.push(file);
				//在页面进行展示
				fileQueued(file, 1);
			} else {
				errorMsgs.push(msg)
			}
		}

		if (errorMsgs.length > 0) {
			//只选择做一个进行上传
			if (len == 1) {
				alert(errorMsgs[0]);

			} else {
				a1 = len;
				a2 = len - errorMsgs.length;

				var replaceArr = new Array(a1, a2);
				alert(stringReplace(jsonLang.t7, replaceArr));
			}
		}

		//拿出第一个，进行上传
		if (!isUploading && uploadQueue.length > 0) uploadFiles(uploadQueue.shift());
	}


	function handleDragOver(evt) {

		evt.stopPropagation();
		evt.preventDefault();
		//防止多次DOM操作
		if (!isDragOver) {
			console.log("Over");
			$('#drag_area').addClass('wf_active').removeClass('wf_normal');
			isDragOver = true;
		}


	}

	function handleDragLeave(evt) {

		console.log("DragLeave");
		evt.stopPropagation();
		evt.preventDefault();
		isDragOver = false;
		$('#drag_area').addClass('wf_normal').removeClass('wf_active');
	}



	function uploadFiles(file) {
		//正在上传
		isUploading = true;
		//设置上传的数据
		var fd = new FormData();
		fd.append("Filename", file.name);
		fd.append("Filedata", file);
		fd.append("Upload", "Submit Query");
		//设置当前的上传对象
		currUploadfile = file;

		if (XHR.readyState > 0) {
			XHR = new XMLHttpRequest();
		}

		XHR.upload.addEventListener("progress", progress, false);
		XHR.upload.addEventListener("load", requestLoad, false);
		XHR.upload.addEventListener("error", error, false);
		XHR.upload.addEventListener("abort", abort, false);
		XHR.upload.addEventListener("loadend", loadend, false);
		XHR.upload.addEventListener("loadstart", loadstart, false);
		XHR.open("POST", config.url);
		XHR.setRequestHeader("Content-Type", "multipart/mixed stream");
		XHR.send(fd);
		XHR.onreadystatechange = function () {

			//只要上传完成不管成功失败
			if (XHR.readyState == 4) {
				console.log("onreadystatechange ", XHR.status, +new Date());

				if (XHR.status == 200) {
					uploadSuccess(currUploadfile, {}, XHR.status)
				} else {
					uploadError()
				}

				//进行下一个上传
				nextUpload()
			}
		};

	}

	//请求完成，无论失败或成功
	function loadend(evt) {
		console.log("loadend", +new Date(), evt);
	}
	//请求开始
	function loadstart(evt) {
		console.log("loadstart", evt);
	}

	//在请求发送或接收数据期间，在服务器指定的时间间隔触发。
	function progress(evt) {
		uploadProgress(currUploadfile, evt.loaded || evt.position, evt.total)
	}

	//在请求被取消时触发，例如，在调用 abort() 方法时。
	function abort(evt) {
		console.log("abort", evt);
	}

	//在请求失败时触发。
	function error(evt) {
		//终止ajax请求
		XHR.abort();
		uploadError()
		nextUpload();
	}

	//在请求成功完成时触发。
	function requestLoad(evt) {
		console.log("requestLoad", +new Date(), evt);
	}

	//进行下一个上传
	function nextUpload() {
		isUploading = false;
		if (uploadQueue.length > 0) {
			uploadFiles(uploadQueue.shift());
		} else {
			//米有正在上传的了
			currUploadfile = {}
		}
	}

	//上传出错误了，比如断网，
	function uploadError() {
		//移除全局变量中的，上传出错的
		removeFileFromFilesUpload(filesUpload, currUploadfile.id)
		var file = currUploadfile;
		$("#handle_button_" + file.id).replaceWith("<dd>" + jsonLang.t8 + "</dd>")
	}


	//对外部注册的函数
	var HTML5Funs = {
		/**
		 * 取消上传
		 * @param string fid 文件的Id 
		 */
		cancelUpload: function (fid) {

			var filesUploadKey = -1;
			var uploadQueueKey = -1;


			//从全局中删除文件
			removeFileFromFilesUpload(filesUpload, fid)

			//如果是正在上传的，AJAX取消
			if (currUploadfile.id == fid) {
				XHR.abort();
			} else {
				//从上传队列中移除
				removeFileFromFilesUpload(uploadQueue, fid)
			}
		}
	}

	window.HTML5Funs = HTML5Funs;


})("FormData" in window && "ondrop" in document.body)