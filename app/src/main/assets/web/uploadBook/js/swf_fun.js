/**
 * swf 上传
 */
var swfu;//swfupload 对象
var swfSelectCount = 0;// 当前选中的文件数量 

window.onload = function () {
	var settings = {
		upload_url: config.url,
		/*post_params: {"PHPSESSID" : "<?php echo session_id(); ?>"},*/
		file_size_limit: config.fileLimitSize + " B",
		file_types: "*." + config.fileTypes.split("|").join(";*."),

		file_types_description: "All Files",
		file_upload_limit: 1000,  //配置上传个数
		file_queue_limit: 0,
		custom_settings: {
			progressTarget: "fsUploadProgress",
			cancelButtonId: "btnCancel"
		},
		debug: 0,

		button_cursor: SWFUpload.CURSOR.HAND,
		button_image_url: "i/wifi_btn_b.png",
		button_width: "240",
		button_height: "100",
		button_float: "right",
		button_placeholder_id: "spanButtonPlaceHolder",
		button_text: '<span class="theFont"></span>',

		assume_success_timeout: 30,
		file_queued_handler: swfFileQueued,
		file_queue_error_handler: fileQueueError,
		file_dialog_complete_handler: fileDialogComplete,
		upload_start_handler: uploadStart,
		upload_progress_handler: uploadProgress,
		upload_error_handler: uploadError,
		upload_success_handler: uploadSuccess,
		upload_complete_handler: uploadComplete,
		queue_complete_handler: queueComplete
	};
	swfu = new SWFUpload(settings);
};



//上传完成
function uploadComplete(file, server, response) {
	//继续下一个文件的上传
	this.startUpload();
}

//完成队列里的上传
function queueComplete(numFilesUploaded) {

}

function userStartUpload(file_id) {
	swfu.startUpload(file_id);
}





function fileQueueError(file, errorCode, message) {
	switch (errorCode) {
		case -100:
		//alert("Over 100 books");
		case -110:
			//alert("One of books is over 500MB");
			break;
		case -120:
			//alert("One of books is 0KB");
			break;
	}
}
//入列完毕
function fileDialogComplete(numFilesSelected, numFilesQueued) {
	if (numFilesSelected > 0) {
		this.startUpload()
	}
}
//开始上传
function uploadStart(file) {
	return true;
}

//上传出错
function uploadError(file, errorCode, message) {
	switch (errorCode) {
		case SWFUpload.UPLOAD_ERROR.HTTP_ERROR:
			errorMessage = "Error";
			break;
		case SWFUpload.UPLOAD_ERROR.UPLOAD_FAILED:
			errorMessage = "Failed";
			break;
		case SWFUpload.UPLOAD_ERROR.IO_ERROR:
			errorMessage = "Please open wifi upload page";
			break;
		case SWFUpload.UPLOAD_ERROR.SECURITY_ERROR:
			errorMessage = "Security error";
			break;
		case SWFUpload.UPLOAD_ERROR.UPLOAD_LIMIT_EXCEEDED:
			errorMessage = "Security error";
			break;
		case SWFUpload.UPLOAD_ERROR.FILE_VALIDATION_FAILED:
			errorMessage = "Unable to verify. Skip ";
			break;
		default:
			errorMessage = "Unhandled error";
			break;
	}

	//从上传队列中移除
	removeFileFromFilesUpload(filesUpload, file.id)

	errorMessage = jsonLang.t8;
	var dd = document.createElement("dd")
	dd.innerHTML = errorMessage
	document.getElementById("handle_button_" + file.id).replaceWith(dd)
}


var tmp = 0;
var errorFile = 0;
var errorMsgs = [];

function swfFileQueued(file) {


	//本次上传选中的文件个数
	if (swfSelectCount == 0) swfSelectCount = this.getStats().files_queued
	//检测文件
	msg = checkFile(file)


	//文件可以通过
	if (!msg) {
		//添加全局的队列
		filesUpload.push(file);
		//在页面进行展示
		fileQueued(file, 0)
	} else {
		//从上传队列移除，验证失败的文件
		this.cancelUpload(file.id, false)
		errorMsgs.push(msg)
	}



	//队列选择完毕,初始化所有的数据
	if (++tmp == swfSelectCount) {

		if (errorMsgs.length > 0) {
			//只选择做一个进行上传
			if (swfSelectCount == 1) {
				alert(errorMsgs[0]);
			} else {
				a1 = swfSelectCount;
				a2 = swfSelectCount - errorMsgs.length;

				var replaceArr = new Array(a1, a2);
				alert(stringReplace(jsonLang.t7, replaceArr));
			}
		}

		tmp = 0;
		errorFile = 0;
		swfSelectCount = 0;
		errorMsgs = []
	}


}



var SWFFuns = {
	cancelUpload: function (fid) {
		swfu.cancelUpload(fid, false);
	}

}