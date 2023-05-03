/**
 * 公共函数
 */
//全局的配置文件 
var config = {
	fileTypes: "txt|epub|umd", //允许上传的文件格式 "txt|epub" // |doc|docx|wps|xls|xlsx|et|ppt|pptx|dps
	//url : "http://"+location.host+"?action=addBook",//"http://localhost/t/post.php",//
	url: "../addLocalBook",
	fileLimitSize : 500 * 1024 *1024

};

//文件对应序号
var fileMap = {};

/**
 * HTML5 和 flash 公用，所有文件对象集合
 * @var array
 */
var filesUpload	= []; //

//初始化表格
init();

function init(){
	//判断浏览器的高度预留空表格
	var tr_num = parseInt(Math.round(((window.innerHeight || document.documentElement.clientHeight) *.5)/43));
	var item =  '<tr data-js="item" data-status="init">' +
		'<td><span></span></td>' +
		'<td><span></span></td>' +
		'<td><span></span></td>' +
		'<td><span><i></i></span></td>' +
		'</tr>';
	var i = 0;
	var HTML = '';
	while(i < tr_num){
		HTML = HTML + item;
		i ++;
	}
	$('#drag table tbody').prepend(HTML);
}

//统计文件大小
function countFileSize(fileSize)
{
	var KB  = 1024;
	var MB = 1024 * 1024;
	if(KB >= fileSize){
	   return fileSize+"B";
	}else if(MB >= fileSize){
		return (fileSize/KB).toFixed(2)+"KB";
	}else{
		return (fileSize/MB).toFixed(2)+"MB";
	}
}

//如果文件太长进行截取
function substr_string(name)
{
	var maxLen = 30;
	var len = name.length;
	if(len < maxLen )return name;

	var lastIndex = name.lastIndexOf(".");
	var suffix    = name.substr(lastIndex);
	var pre       = name.substr(0,lastIndex);
	var preLen    = pre.length;
	var preStart  = preLen - 20;
	//前面10个 + 后面5个
	var fileName  =  pre.substr(0,20) + "...." + pre.substr( preStart > 4 ? -4 : -preStart , 4)+suffix;
	return fileName
}


function checkFile(file) {
	if (!file.name || !file.name.toLowerCase().match('('+config.fileTypes+')$')) {
		return "格式不支持";
	}

	var len = filesUpload.length;
	for(var i=0; i< len; i++){
		if(filesUpload[i].name == file.name)	{
			return "文件已存在";
		}
	}
	return null;
}

/**
 * 添加文件时，回调的函数
 * @param object file 文件对象
 * @param int type 0 是swf 上传的，1 是html5上传的
 */
function fileQueuedPC(file, type)
{
	var size=0 ,fid=file.id, name="";
	type = type || 0;

	if(file != undefined )
	{
		//计算文件大小 单位MB
		size = countFileSize(file.size);
		name = substr_string(file.name);

		//如果没有找到这个节点，先创建
		if ($('#drag tbody tr:last-child').prev().attr('data-status') != 'init' ){
			var HTML =  '<tr data-js="item" data-status="init">' +
				'<td><span></span></td>' +
				'<td><span></span></td>' +
				'<td><span></span></td>' +
				'<td><span><i></i></span></td>' +
				'</tr>';

			$("#drag tbody tr:last-child").before(HTML);
		}

		var i = $('#drag [data-status=init]').eq(0).index('#drag [data-js=item]');

		$('#drag [data-js=item]').eq(i).children().eq(0).find('span').html(i+1);
		$('#drag [data-js=item]').eq(i).children().eq(1).find('span').html(name);
		$('#drag [data-js=item]').eq(i).children().eq(2).find('span').html(size);
		$('#drag [data-js=item]').eq(i).children().eq(3).find('span i').addClass('red').html('0%');
		$('#drag [data-js=item]').eq(i).attr('data-status', 'ed');

		fileMap[file.name] = $('#drag [data-js=item]').eq(i).children().eq(3).find('span i');

	}
}

//上传时返回的状态
function uploadProgress(file, bytesLoaded, bytesTotal)
{
	fileMap[file.name].html(parseInt((bytesLoaded/bytesTotal)*100)+"%");
}


//上传成功
function uploadSuccess(file, serverData, res)
{
	fileMap[file.name].removeClass('red').addClass('op_right').html('');
}

/**
 * 查找在数组中的位置
 */
function findObjectKey (object, fid){
	var len = object.length; 
	for(var i=0; i<len; i++){
		if(object[i].id == fid){
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
function removeFileFromFilesUpload(files, fid){
	//console.log(currUploadfile);

	var filesUploadKey = -1;
	
	filesUploadKey = findObjectKey(files, fid);
	//从全局文件中移除
	if(filesUploadKey > -1)
		 files.splice(filesUploadKey, 1);

	return files;
}