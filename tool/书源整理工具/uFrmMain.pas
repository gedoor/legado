unit uFrmMain;

interface

uses
  iocp.Http.Client, iocp.Utils.Str, DateUtils, RegularExpressions,
  YxdJson, YxdStr, YxdHash, YxdWorker, ShellAPI, Math, StrUtils,
  uBookSourceBean,
  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms,
  Dialogs, StdCtrls, ExtCtrls, Menus, SynEdit, SynMemo, ComCtrls, SyncObjs,
  SynEditHighlighter, SynHighlighterJSON, Vcl.Buttons;

type
  PProcessState = ^TProcessState;
  TProcessState = record
    STime: Int64;
    NeedFree: Boolean;
    Min: Integer;
    Max: Integer;
    Value: Integer;
  end;

type
  TForm1 = class(TForm)
    Panel1: TPanel;
    SrcList: TListBox;
    Splitter1: TSplitter;
    Panel2: TPanel;
    PopupMenu1: TPopupMenu;
    C1: TMenuItem;
    Panel3: TPanel;
    EditData: TSynMemo;
    Button1: TButton;
    Panel4: TPanel;
    lbCount: TLabel;
    bookGroupList: TComboBox;
    StatusBar1: TStatusBar;
    ProgressBar1: TProgressBar;
    SynJSONSyn1: TSynJSONSyn;
    PopupMenu2: TPopupMenu;
    S1: TMenuItem;
    N1: TMenuItem;
    C2: TMenuItem;
    X1: TMenuItem;
    P1: TMenuItem;
    A1: TMenuItem;
    N2: TMenuItem;
    N3: TMenuItem;
    R1: TMenuItem;
    Z1: TMenuItem;
    N4: TMenuItem;
    W1: TMenuItem;
    Label1: TLabel;
    Edit1: TEdit;
    CheckBox1: TCheckBox;
    CheckBox2: TCheckBox;
    D1: TMenuItem;
    N6: TMenuItem;
    C3: TMenuItem;
    N5: TMenuItem;
    S2: TMenuItem;
    G1: TMenuItem;
    N7: TMenuItem;
    E1: TMenuItem;
    Timer1: TTimer;
    CheckBox3: TCheckBox;
    MainMenu1: TMainMenu;
    F1: TMenuItem;
    H1: TMenuItem;
    E2: TMenuItem;
    I1: TMenuItem;
    SaveDialog1: TSaveDialog;
    W2: TMenuItem;
    N8: TMenuItem;
    R2: TMenuItem;
    N9: TMenuItem;
    H2: TMenuItem;
    E3: TMenuItem;
    H3: TMenuItem;
    N10: TMenuItem;
    N11: TMenuItem;
    T1: TMenuItem;
    Panel5: TPanel;
    Splitter2: TSplitter;
    Label2: TLabel;
    edtLog: TSynMemo;
    SpeedButton1: TSpeedButton;
    StaticText1: TStaticText;
    O1: TMenuItem;
    N12: TMenuItem;
    OpenDialog1: TOpenDialog;
    A2: TMenuItem;
    C4: TMenuItem;
    X2: TMenuItem;
    P2: TMenuItem;
    N13: TMenuItem;
    B1: TMenuItem;
    F2: TMenuItem;
    N14: TMenuItem;
    CheckBox4: TCheckBox;
    N15: TMenuItem;
    N16: TMenuItem;
    N17: TMenuItem;
    N18: TMenuItem;
    S3: TMenuItem;
    B2: TMenuItem;
    B3: TMenuItem;
    URLU1: TMenuItem;
    N19: TMenuItem;
    URL1: TMenuItem;
    RadioButton1: TRadioButton;
    RadioButton2: TRadioButton;
    RadioButton3: TRadioButton;
    RadioButton4: TRadioButton;
    CheckBox5: TCheckBox;
    N20: TMenuItem;
    U1: TMenuItem;
    W3: TMenuItem;
    Panel6: TPanel;
    Button2: TButton;
    Button3: TButton;
    Button4: TButton;
    Button5: TButton;
    Shape1: TShape;
    CheckBox6: TCheckBox;
    O2: TMenuItem;
    procedure FormCreate(Sender: TObject);
    procedure C1Click(Sender: TObject);
    procedure FormShow(Sender: TObject);
    procedure SrcListKeyDown(Sender: TObject; var Key: Word;
      Shift: TShiftState);
    procedure FormDestroy(Sender: TObject);
    procedure SrcListData(Control: TWinControl; Index: Integer;
      var Data: string);
    procedure PopupMenu2Popup(Sender: TObject);
    procedure R1Click(Sender: TObject);
    procedure Z1Click(Sender: TObject);
    procedure C2Click(Sender: TObject);
    procedure X1Click(Sender: TObject);
    procedure P1Click(Sender: TObject);
    procedure A1Click(Sender: TObject);
    procedure S1Click(Sender: TObject);
    procedure SrcListClick(Sender: TObject);
    procedure EditDataChange(Sender: TObject);
    procedure W1Click(Sender: TObject);
    procedure D1Click(Sender: TObject);
    procedure C3Click(Sender: TObject);
    procedure Button1Click(Sender: TObject);
    procedure Timer1Timer(Sender: TObject);
    procedure bookGroupListChange(Sender: TObject);
    procedure N7Click(Sender: TObject);
    procedure E1Click(Sender: TObject);
    procedure SrcListDblClick(Sender: TObject);
    procedure I1Click(Sender: TObject);
    procedure E2Click(Sender: TObject);
    procedure W2Click(Sender: TObject);
    procedure R2Click(Sender: TObject);
    procedure S2Click(Sender: TObject);
    procedure G1Click(Sender: TObject);
    procedure H2Click(Sender: TObject);
    procedure T1Click(Sender: TObject);
    procedure SpeedButton1Click(Sender: TObject);
    procedure O1Click(Sender: TObject);
    procedure A2Click(Sender: TObject);
    procedure PopupMenu1Popup(Sender: TObject);
    procedure C4Click(Sender: TObject);
    procedure X2Click(Sender: TObject);
    procedure P2Click(Sender: TObject);
    procedure B1Click(Sender: TObject);
    procedure F2Click(Sender: TObject);
    procedure N14Click(Sender: TObject);
    procedure N15Click(Sender: TObject);
    procedure N17Click(Sender: TObject);
    procedure S3Click(Sender: TObject);
    procedure N18Click(Sender: TObject);
    procedure B2Click(Sender: TObject);
    procedure URL1Click(Sender: TObject);
    procedure RadioButton1Click(Sender: TObject);
    procedure RadioButton2Click(Sender: TObject);
    procedure RadioButton3Click(Sender: TObject);
    procedure RadioButton4Click(Sender: TObject);
    procedure U1Click(Sender: TObject);
    procedure W3Click(Sender: TObject);
    procedure Button2Click(Sender: TObject);
    procedure Button3Click(Sender: TObject);
    procedure Button4Click(Sender: TObject);
    procedure Button5Click(Sender: TObject);
    procedure O2Click(Sender: TObject);
    procedure Button6Click(Sender: TObject);
  private
    { Private declarations }
    OldListWndProc, OldTextWndProc: TWndMethod;
    FBookSrcData: JSONArray;
    FBookCopyData: JSONArray;
    FBookGroups: TStringHash;
    FIsChange, FChanging: Boolean;
    FCurIndex: Integer;

    FWaitStop: Integer;
    FTaskRef: Integer;
    FTaskStartTime: Int64;
    FCheckLastTime: Int64;
    FMaxWorkers: Integer;

    FFilterList: TList;
    FCurCheckIndex: Integer;
    FCheckCount, FCheckFinish: Integer;
    FAutoFind, FAutoDel, FAllSrc, FAutoScore: Boolean;

    FWaitCheckBookSourceSingId: Integer;

    FLocker: TCriticalSection;
    FStateMsg: string;
    FLastSortFlag: Integer;
    FLastFindSource: string;
    FBookType: string;
    procedure SetBookType(const Value: string);

  protected
    procedure SrcListWndProc(var Message: TMessage);
    procedure SrcListTextWndProc(var Message: TMessage);
    procedure AddSrcFiles(ADrop: Integer);
    procedure WMDropFiles(var Msg: TWMDropFiles); message WM_DROPFILES;
  public
    { Public declarations }
    function CheckItem(Item: TBookSourceItem): Boolean;
    function CheckSaveState(): Boolean;


    function CheckBookSourceItem(Item: TBookSourceItem; RaiseErr: Boolean = False; OutLog: TStrings = nil): Boolean; overload;
    function CheckBookSourceItem(Item: TBookSourceItem; Http: THttpClient; Header: THttpHeaders; RaiseErr: Boolean = False; OutLog: TStrings = nil): Boolean; overload;

    procedure AddSrcFile(const FileName: string);
    procedure UpdateBookGroup(Item: TBookSourceItem);
    
    procedure RemoveRepeat(AJob: PJob);  
    procedure WaitCheckBookSource(AJob: PJob);
    procedure DoCheckBookSourceItem(AJob: PJob);
    
    procedure TaskFinish(AJob: PJob);
    procedure DoNotifyDataChange(AJob: PJob);
    procedure DoUpdateProcess(AJob: PJob);

    procedure Log(const Msg: string);
    procedure LogD(const Msg: string);
    procedure DispLog();
    procedure NotifyListChange(Flag: Integer = 0);

    procedure RemoveSelected();
    procedure CopySelected();
    procedure Paste();
    procedure CutSelectedt();
    procedure FindSource(const FindStr: string);
    procedure EditSource(Item: TBookSourceItem);
    procedure ExportSelectedToFile();

    procedure ChangeItemIndex(const CurIndex, NewIndex: Integer);

    property BookType: string read FBookType write SetBookType;
  end;

var
  Form1: TForm1;

implementation

{$R *.dfm}

uses
  uFrmWait, uFrmEditSource, uFrmReplaceGroup;

function GetCurJavaDateTime(): Int64;
begin
   Result := Round(((Now - 25569) * 86400000 - 3600000 * 8) / 1000);
end;

procedure CutOrCopyFiles(FileList: AnsiString; bCopy: Boolean);
type
  PDropFiles = ^TDropFiles;
 
  TDropFiles = record
    pfiles: DWORD;
    pt: TPoint;
    fNC: BOOL;
    fwide: BOOL;
  end;
const
  DROPEFFECT_COPY = 1;
  DROPEFFECT_MOVE = 2;
var
  hGblFileList: hGlobal;
  pFileListDate: Pbyte;
  HandleDropEffect: UINT;
  hGblDropEffect: hGlobal;
  pdwDropEffect: PDWORD;
  iLen: Integer;
begin
  iLen := Length(FileList) + 2;
  FileList := FileList + #0#0;
  hGblFileList := GlobalAlloc(GMEM_ZEROINIT or GMEM_MOVEABLE or GMEM_SHARE,
    SizeOf(TDropFiles) + iLen);
  pFileListDate := GlobalLock(hGblFileList);
  PDropFiles(pFileListDate)^.pfiles := SizeOf(TDropFiles);
  PDropFiles(pFileListDate)^.pt.Y := 0;
  PDropFiles(pFileListDate)^.pt.X := 0;
  PDropFiles(pFileListDate)^.fNC := False;
  PDropFiles(pFileListDate)^.fwide := False;
  Inc(pFileListDate, SizeOf(TDropFiles));
  CopyMemory(pFileListDate, @FileList[1], iLen);
  GlobalUnlock(hGblFileList);
  HandleDropEffect := RegisterClipboardFormat('Preferred DropEffect ');
  hGblDropEffect := GlobalAlloc(GMEM_ZEROINIT or GMEM_MOVEABLE or GMEM_SHARE,
    SizeOf(DWORD));
  pdwDropEffect := GlobalLock(hGblDropEffect);
  if (bCopy) then pdwDropEffect^ := DROPEFFECT_COPY
  else pdwDropEffect^ := DROPEFFECT_MOVE;
  GlobalUnlock(hGblDropEffect);
  if OpenClipboard(0) then begin
    EmptyClipboard();
    SetClipboardData(HandleDropEffect, hGblDropEffect);
    SetClipboardData(CF_HDROP, hGblFileList);
    CloseClipboard();
  end;
end;

// 复制文件，多个文件以 #0 分隔
procedure CopyFileClipbrd(const FName: string);
begin
  CutOrCopyFiles(AnsiString(FName), True);
end;

procedure TForm1.AddSrcFiles(ADrop: Integer);
var
  i: Integer;
  p: array[0..1023] of Char;
begin
  for i := 0 to DragQueryFile(ADrop, $FFFFFFFF, nil, 0) - 1 do begin
    DragQueryFile(ADrop, i, p, 1024);
    AddSrcFile(StrPas(p));
  end;
end;

procedure TForm1.B1Click(Sender: TObject);
var
  FindStr, NewStr: string;
  I, Flag: Integer;
  Item: TBookSourceItem;
begin
  Flag := 1;
  if ShowReplaceGroup(Self, '替换 - 书源名称', FindStr, NewStr, Flag) then begin
    if (FindStr <> '') and (Flag = 0) then
      Exit;
    for I := 0 to FBookSrcData.Count - 1 do begin
      Item := TBookSourceItem(FBookSrcData.O[I]);
      if not Assigned(Item) then Continue;
      Item.bookSourceName := StringReplace(Trim(Item.bookSourceName), FindStr, NewStr, [rfReplaceAll, rfIgnoreCase]);
    end;
    NotifyListChange();
  end;
end;

procedure TForm1.B2Click(Sender: TObject);
var
  FindStr, NewStr: string;
  I, Flag: Integer;
  Item: TBookSourceItem;
begin
  Flag := 1;
  if ShowReplaceGroup(Self, '替换 - 书源URL', FindStr, NewStr, Flag) then begin
    if (FindStr <> '') and (Flag = 0) then
      Exit;
    for I := 0 to FBookSrcData.Count - 1 do begin
      Item := TBookSourceItem(FBookSrcData.O[I]);
      if not Assigned(Item) then Continue;
      Item.bookSourceUrl := StringReplace(Trim(Item.bookSourceUrl), FindStr, NewStr, [rfReplaceAll, rfIgnoreCase]);
    end;
    NotifyListChange();
  end;
end;

procedure TForm1.bookGroupListChange(Sender: TObject);
begin
  if FChanging then
    Exit; 
  FChanging := True;
  try     
    EditData.Text := '';
    FIsChange := False;
    FCurIndex := -1;
    NotifyListChange(1);
  finally
    FChanging := False;
  end;
end;

procedure TForm1.Button1Click(Sender: TObject);
begin
  if Button1.Tag = 0 then begin
    Button1.Tag := 1;
    Button1.Caption := '停止(&S)';
    
    FTaskRef := 0;
    FWaitStop := 0;
    FCheckLastTime := GetTimestamp;
    FTaskStartTime := GetTimestamp;
    FMaxWorkers := Max(1, StrToIntDef(Edit1.Text, 8));
    Workers.MaxWorkers := FMaxWorkers;

    edtLog.Lines.Clear;
    Log('正在初始化任务...');

    Timer1.Enabled := True;
    ProgressBar1.Min := 0;
    ProgressBar1.Max := 100;
    ProgressBar1.Position := 0;

    FAutoFind := CheckBox4.Checked;
    FAutoDel := CheckBox1.Checked;
    FAllSrc := CheckBox5.Checked;
    FAutoScore := CheckBox6.Checked;

    if FAutoDel or FAutoFind then begin
      Inc(FTaskRef);
      Log('正在处理...');
      Workers.Post(RemoveRePeat, Pointer(Integer(CheckBox3.Checked)));
    end;
    if CheckBox2.Checked then begin
      Inc(FTaskRef);
      if not CheckBox1.Checked then
        Workers.SendSignal(FWaitCheckBookSourceSingId);
    end;

    ShowWaitDlg();
  end else begin
    AtomicIncrement(FTaskRef);
    if AtomicDecrement(FTaskRef) <= 0 then begin
      Button1.Tag := 0;
      Button1.Caption := '开始处理(&B)';
      Timer1.Enabled := False;
      FTaskStartTime := 0;
      ProgressBar1.Visible := False;
      HideWaitDlg;
      Log('任务结束');
    end else begin        
      Button1.Tag := 2;
      Button1.Caption := '正在停止...';      
      AtomicIncrement(FWaitStop);
    end;
  end;
  //Application.ProcessMessages;
end;

procedure TForm1.Button2Click(Sender: TObject);
begin
  ChangeItemIndex(SrcList.ItemIndex, 0);
end;

procedure TForm1.Button3Click(Sender: TObject);
begin
  ChangeItemIndex(SrcList.ItemIndex, SrcList.ItemIndex - 1);
end;

procedure TForm1.Button4Click(Sender: TObject);
begin
  ChangeItemIndex(SrcList.ItemIndex, SrcList.ItemIndex + 1);
end;

procedure TForm1.Button5Click(Sender: TObject);
begin
  ChangeItemIndex(SrcList.ItemIndex, SrcList.Count - 1);
end;

procedure TForm1.Button6Click(Sender: TObject);
var
  url: string;
begin
  url := 'http://www.baidu.com/<searchPage+1>?aaa=0';
  url := TRegEx.Replace(url, '<searchPage([-+]1)>', '{{page$1}}', [roIgnoreCase]);
  ShowMessage(url);
end;

procedure TForm1.A1Click(Sender: TObject);
begin
  EditData.SelectAll;
end;

procedure TForm1.A2Click(Sender: TObject);
begin
  OpenDialog1.Title := '添加书源文件';
  if OpenDialog1.Execute(Handle) then
    AddSrcFile(OpenDialog1.FileName);
end;

procedure TForm1.AddSrcFile(const FileName: string);
var
  Data: JSONArray;
  Item: TBookSourceItem;
  I: Integer;
begin
  Data := JSONArray.Create;
  try    
    Data.LoadFromFile(FileName);
    I := Data.Count;
    while I > 0 do begin
      try
        Item := TBookSourceItem(Data.O[0]);
        if Assigned(Item) and (Item.bookSourceUrl <> '') then begin
          UpdateBookGroup(Item);
          FBookSrcData.Add(Data.O[0]);
        end;
      except
      end;
      Dec(I);
    end;
  finally
    Data.Free;
    NotifyListChange;
  end;
end;

procedure TForm1.C1Click(Sender: TObject);
begin
  FBookSrcData.Clear;
  NotifyListChange;
end;

procedure TForm1.C2Click(Sender: TObject);
begin
  EditData.CopyToClipboard;
end;

procedure TForm1.C3Click(Sender: TObject);
var
  S: string;
  Item, NewItem: TBookSourceItem;
begin
  if SrcList.ItemIndex < 0 then Exit;
  if not CheckSaveState then Exit;
  Item := TBookSourceItem(FFilterList[SrcList.ItemIndex]);
  S := InputBox('书源名称', '请输入书源名称', Item.bookSourceName);

  NewItem := TBookSourceItem(FBookSrcData.AddChildObject);
  NewItem.Parse(Item.ToString);
  NewItem.bookSourceName := S;

  NotifyListChange;
  if FFilterList.Count > 0 then    
    SrcList.ItemIndex := FFilterList.Count - 1;
end;

procedure TForm1.C4Click(Sender: TObject);
begin
  CopySelected;
end;

function TForm1.CheckBookSourceItem(Item: TBookSourceItem; RaiseErr: Boolean; OutLog: TStrings): Boolean;
var
  Http: THttpClient;
  Header: THttpHeaders;
begin
  Result := False;
  try
    if not Assigned(Item) then Exit; 
    Http := THttpClient.Create(nil);
    if Assigned(OutLog) then begin    
      Http.ConnectionTimeOut := 6000;
      Http.RecvTimeOut := 30000;
    end else begin
      Http.ConnectionTimeOut := 30000;
      Http.RecvTimeOut := 30000;
    end;
    Header := THttpHeaders.Create;

    FAutoScore := True;
    Result := CheckBookSourceItem(Item, Http, Header, RaiseErr, OutLog);
    
  finally
    FreeAndNil(Http);
    FreeAndNil(Header);
  end;
end;

procedure TForm1.ChangeItemIndex(const CurIndex, NewIndex: Integer);
var
  Item1, Item2: TBookSourceItem;
  I1, I2, NIndex: Integer;
begin
  if CurIndex < 0 then Exit;
  if SrcList.Count <= 1 then Exit;
  NIndex := NewIndex;
  if NIndex >= SrcList.Count then
    NIndex := SrcList.Count - 1;
  if NIndex < 0 then Exit;
  if NIndex = CurIndex then Exit;

  Item1 := TBookSourceItem(FFilterList[CurIndex]);
  Item2 := TBookSourceItem(FFilterList[NIndex]);

  I1 := FBookSrcData.IndexOfObject(Item1);
  I2 := FBookSrcData.IndexOfObject(Item2);

  if I1 < 0 then Exit;
  if I2 < 0 then Exit;

  FBookSrcData.MoveItem(I1, I2);
  FCurIndex := NIndex;
  NotifyListChange(1);
end;

function TForm1.CheckBookSourceItem(Item: TBookSourceItem; Http: THttpClient;
  Header: THttpHeaders; RaiseErr: Boolean; OutLog: TStrings): Boolean;

  function ValidationURL(const BaseURL, URL: string): string;
  begin
    if (BaseURL <> '') and (BaseURL <> '-') and (URL <> '-') and (Pos('://', URL) < 1) then begin
      if URL = '' then
        Result := BaseURL
      else if URL[1] = '/' then begin
        if BaseURL[Length(BaseURL)] = '/' then
          Result := BaseURL + URL.Substring(2)
        else
          Result := BaseURL + URL
      end else begin
        if BaseURL[Length(BaseURL)] = '/' then
          Result := BaseURL + URL
        else
          Result := BaseURL + '/' + URL;
      end;
    end else
      Result := URL;
    Result := StringReplace(Result, '=searchPage', '=1', [rfReplaceAll, rfIgnoreCase]);
  end;

  function CheckURL(const URL, Title: string; RaiseErr: Boolean = False; Try404: Boolean = False): Boolean;
  var
    Resp: THttpResult;
    Msg: string;
  begin
    Result := (URL <> '') and (URL <> '-') and (Pos('http', LowerCase(URL)) = 1); 
    if Result then begin
      try
        Resp := Http.Get(UrlEncodeEx(URL), nil, Header);
        if (Resp.StatusCode = 200) or (Try404 and (Resp.StatusCode >= 400) and (Resp.StatusCode < 500)) then begin
          Result := True;
          if Assigned(OutLog) then OutLog.Add(Title + '连接成功.');
        end else begin
          Result := False;
          Msg := Format('%s测试失败(StatusCode: %d, %s).', [Title, Resp.StatusCode, URL]);
          if Assigned(OutLog) then OutLog.Add(Msg);
          if RaiseErr then
            raise Exception.Create(Msg);
        end;
      except
        Result := False;
        Msg := Format('%s测试出错(%s).', [Title, Exception(ExceptObject).Message]);
        if Assigned(OutLog) then OutLog.Add(Msg);
        if RaiseErr then
          raise Exception.Create(Msg);
      end;
    end else if Assigned(OutLog) then              
      OutLog.Add('无效的' + Title + '.');
  end;

  // 检测发现列表
  function CheckFindURL(AItem: TBookSourceItem; const BaseURL, Text, Title: string; RaiseErr: Boolean): Boolean;
  var 
    I, J, L: Integer;
    Msg, Item, SubTitle, AURL: string;
  begin
    Result := False;
    if Text = '' then begin
      Result := True;
      Exit;
    end;
    try
      J := 1;
      while (J > 0) and (J <= Length(Text)) do begin        
        I := PosEx('&&', Text, J);
        L := 2;
        if I <= 0 then begin
          I := PosEx(#$A, Text, J);
          L := 1;
        end;
        if I > 0 then begin
          Item := MidStr(Text, J, I - J);
          J := I + L;
        end else begin
          Item := Trim(RightStr(Text, Length(Text) - J + 1));
          J := Length(Text) + 1;
        end;

        if (Item = #$A) or (Item = #13) then
          Continue;

        Item := StringReplace(Item, #13, '', [rfReplaceAll]);
        Item := StringReplace(Item, #10, '', [rfReplaceAll]);
        Item := StringReplace(Item, '\n', '', [rfReplaceAll, rfIgnoreCase]);
        Item := Trim(Item);  
              
        I := Pos('::', Item);
        if (Item = '') or (I < 1) then begin
          if FAutoScore then
            AItem.score := AItem.score - 20;
          if Assigned(OutLog) then
            OutLog.Add('发现列表格式错误');
          Continue;
        end else begin
          SubTitle := Trim(LeftStr(Item, I - 1));
          AURL := Trim(RightStr(Item, Length(Item) - I - 1));
          if not CheckURL(ValidationURL(BaseURL, AURL), '发现列表项【' + SubTitle + '】') then
            AItem.score := AItem.score - 1;
        end;
      end;
    except
      Result := False;
      Msg := Format('%s测试出错(%s).', [Title, Exception(ExceptObject).Message]);
      if Assigned(OutLog) then OutLog.Add(Msg);
      if RaiseErr then
        raise Exception.Create(Msg);
    end;
  end;

var
  T: Int64;
  AScore, I: Integer;
  URL: string;
  Json: JSONObject;
begin
  Result := False;
  if not Assigned(Item) then Exit;
  Json := JSONObject.Create;
  try
    if Item.bookSourceUrl <> '' then begin
      T := GetTimestamp;
      if FAutoScore then
        Item.score := 0;
      Header.Clear;
      Header.Add('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36');
      if Item.header <> '' then begin
        if Json.Parse(Item.header, True) then begin
          for I := 0 to Json.Count do
            Header.Add(Json.Items[i].FName, Json.Items[i].AsString);
        end;
      end;

      // 检测书源URL
      Result := CheckURL(Trim(Item.bookSourceUrl), '书源URL', RaiseErr, True);

      if Result then begin
        if FAutoScore then
          Item.score := Item.score + 50;
        // 检测搜索URL
        URL := Trim(Item.searchUrl);
        if CheckURL(ValidationURL(Trim(Item.bookSourceUrl), URL), '搜索地址', RaiseErr) then begin
          if FAutoScore then
            Item.score := Item.score + 50;
        end;
        // 检测发现列表
        if FAutoScore or (Assigned(OutLog)) then
          CheckFindURL(Item, Trim(Item.bookSourceUrl), Trim(Item.exploreUrl), '发现', RaiseErr);
      end;

      if Assigned(OutLog) then
        OutLog.Add(Format('用时 %d ms.', [GetTimestamp - T]));
    end else begin
      if Assigned(OutLog) then
        OutLog.Add('书源URL未设置.');
      raise Exception.Create('书源URL无效');
    end;
  finally
    FreeAndNil(Json);
  end;
end;

function TForm1.CheckItem(Item: TBookSourceItem): Boolean;
begin
  Result := Assigned(Item) and (Item.bookSourceUrl <> '');
end;

function TForm1.CheckSaveState: Boolean;
var
  LR: Integer;
begin
  if FIsChange and (FCurIndex >= 0) and (FCurIndex < SrcList.Count) then begin
    LR := MessageBox(Handle, '书源内容已经修改，是否保存？', '提示', 64 + MB_YESNOCANCEL);
    if LR = IDCANCEL then begin
      Result := False;
      Exit;
    end;
    if LR = IDYES then
      S1Click(S1);
  end;
  Result := True;
end;

procedure TForm1.CopySelected;
var
  I: Integer;
  Item: JSONObject;
begin
  FBookCopyData.Clear;
  for I := SrcList.Count - 1 downto 0 do begin
    if SrcList.Selected[I] then begin
      Item := FBookCopyData.AddChildObject;
      Item.Assign(JSONObject(FFilterList[I]));
    end;
  end;
end;

procedure TForm1.CutSelectedt;
begin
  CopySelected;
  RemoveSelected;
end;

procedure TForm1.D1Click(Sender: TObject);
begin
  RemoveSelected;
end;

procedure TForm1.DispLog;
begin
  if FTaskStartTime > 0 then begin 
    if ProgressBar1.Visible then
      StatusBar1.Panels[1].Text := Format('%s (%d/%d, %d%%) (用时: %dms)', 
        [FStateMsg, ProgressBar1.Position, ProgressBar1.Max, Round(ProgressBar1.Position / ProgressBar1.Max * 100),
         GetTimestamp - FTaskStartTime])
    else  
      StatusBar1.Panels[1].Text := Format('%s (用时: %dms)', [FStateMsg, GetTimestamp - FTaskStartTime])
  end else
    StatusBar1.Panels[1].Text := FStateMsg;
end;

procedure TForm1.DoCheckBookSourceItem(AJob: PJob);
var
  Item: TBookSourceItem;
  State: PProcessState;
  V: Integer;
  IsOK: Boolean;
  Http: THttpClient;
  Header: THttpHeaders;
begin
  V := 0;
  try
    Http := THttpClient.Create(nil);
    Http.ConnectionTimeOut := 30000;
    Http.RecvTimeOut := 30000;
    Http.AllowCookies := False;
    Header := THttpHeaders.Create;
    
    while (not AJob.IsTerminated) and (FWaitStop = 0) do begin
      V := AtomicIncrement(FCurCheckIndex) - 1;
      
      FLocker.Enter;
      if (GetTimestamp - FCheckLastTime) > 100 then begin
        FCheckLastTime := GetTimestamp;
        New(State);
        State.Min := 0;
        State.Max := FCheckCount;
        State.Value := FCheckFinish;
        Workers.Post(DoUpdateProcess, State, True);
        Sleep(10);
      end;
      FLocker.Leave;
      
      if V < FCheckCount then begin
        Item := TBookSourceItem(FBookSrcData.O[V]);
        if not Assigned(Item) then Exit;

        if (FAllSrc) or (FFilterList.IndexOf(Item) >= 0) then begin

          try
            IsOK := CheckBookSourceItem(Item, Http, Header);
          except
            IsOK := False;
          end;

          if IsOK then
            Item.RemoveGroup('失效')
          else
            Item.AddGroup('失效');

          Sleep(50);
        end;

        AtomicIncrement(FCheckFinish);
      end else
        Break;

    end;
  finally

    if (V = FCheckCount) or (FWaitStop > 0) then begin
      Sleep(100);
      while not AJob.IsTerminated do begin
        if (FCheckFinish >= FCheckCount) or (FWaitStop > 0) then
          Break;

        FLocker.Enter;
        if (GetTimestamp - FCheckLastTime) > 1000 then begin
          FCheckLastTime := GetTimestamp;
          New(State);
          State.Min := 0;
          State.Max := FCheckCount;
          State.Value := FCheckFinish;
          Workers.Post(DoUpdateProcess, State, True);
          Sleep(10);
        end;
        FLocker.Leave;

        Sleep(100);
      end;
      Workers.Post(TaskFinish, nil, True);
    end;

    FreeAndNil(Http);
    FreeAndNil(Header);
  end;
end;

procedure TForm1.DoNotifyDataChange(AJob: PJob);
begin
  NotifyListChange;
end;

procedure TForm1.DoUpdateProcess(AJob: PJob);
var
  V: PProcessState;
begin
  if not Assigned(Self) then Exit;  
  V := AJob.Data;
  if V = nil then
    ProgressBar1.Visible := False
  else begin
    ProgressBar1.Min := V.Min;
    ProgressBar1.Max := V.Max;
    ProgressBar1.Position := V.Value;
    ProgressBar1.Visible := Button1.Tag <> 0;
    if V.NeedFree then
      Dispose(V);
  end;
end;

procedure TForm1.E1Click(Sender: TObject);
begin
  if SrcList.ItemIndex < 0 then Exit;
  EditSource(TBookSourceItem(FFilterList[SrcList.ItemIndex]));
end;

procedure TForm1.E2Click(Sender: TObject);
var
  FName: JSONString;
begin
  if SaveDialog1.Execute(Handle) then begin
    FName := SaveDialog1.FileName;
    if ExtractFileExt(FName) = '' then
      FName := FName + '.json';
    FBookSrcData.SaveToFile(FName, 4, YxdStr.TTextEncoding.teUTF8, False);
  end;
end;

procedure TForm1.EditDataChange(Sender: TObject);
begin
  FIsChange := True;
end;

procedure TForm1.EditSource(Item: TBookSourceItem);
begin
  ShowEditSource(Item,
    procedure (Item: TBookSourceItem) 
    begin
      if FBookSrcData.IndexOfObject(Item) < 0 then
        FBookSrcData.Add(JSONObject(Item));  
      NotifyListChange;
      if (FCurIndex >= 0) and (FCurIndex < FFilterList.Count) then begin
        if TObject(FFilterList[FCurIndex]) = Item then begin        
          EditData.Text := TBookSourceItem(FFilterList[FCurIndex]).ToString(4);
          FIsChange := False;
        end;
      end;  
    end
  );
end;

procedure TForm1.ExportSelectedToFile;
var
  FName: JSONString;
  I: Integer;
  Item: TBookSourceItem;
  Items: JSONArray;
begin
  if SrcList.SelCount = 0 then
    Exit;
  if SaveDialog1.Execute(Handle) then begin
    FName := SaveDialog1.FileName;
    if ExtractFileExt(FName) = '' then
      FName := FName + '.json';
    Items := JSONArray.Create;
    try
      for I := 0 to SrcList.Count - 1 do begin
        if SrcList.Selected[I] then begin
          Item := TBookSourceItem(JSONObject(FFilterList[I]));
          if not Assigned(Item) then Continue;
          Items.AddChildObject.Assign(Item);
        end;
      end;
      Items.SaveToFile(FName, 4, YxdStr.TTextEncoding.teUTF8, False);
    finally
      Items.Free;
    end;
  end;
end;

procedure TForm1.F2Click(Sender: TObject);
begin
  FindSource(InputBox('查找书源', '输入要查找的关键字', ''));
end;

procedure TForm1.FindSource(const FindStr: string);
var
  I, J: Integer;
  Item: TBookSourceItem;
begin
  if FindStr = '' then Exit;
  FLastFindSource := FindStr;
  J := SrcList.ItemIndex + 1;
  if J < 0 then J := 0;
  for I := J to SrcList.Count - 1 do begin
    Item := TBookSourceItem(JSONObject(FFilterList[I]));
    if not Assigned(Item) then Continue;
    J := Pos(FindStr, Item.bookSourceName);
    if (Pos(FindStr, Item.bookSourceName) > 0) or (URL1.Checked and (Pos(FindStr, Item.bookSourceUrl) > 0)) then begin
      SrcList.ClearSelection;
      SrcList.ItemIndex := I;
      SrcList.Selected[I] := True;
      SrcList.ScrollBy(0, SrcList.ItemHeight * I + 1);
      Exit;
    end;
  end;
  LogD('找不到 "' + FindStr + '" 了');
end;

procedure TForm1.FormCreate(Sender: TObject);
begin
  JsonNameAfterSpace := True;
  JsonCaseSensitive := False;
  FBookCopyData := JSONArray.Create;
  FBookSrcData := JSONArray.Create;
  FBookGroups := TStringHash.Create(997);
  FFilterList := TList.Create;
  FLocker := TCriticalSection.Create;

  FWaitCheckBookSourceSingId := Workers.RegisterSignal('WaitCheckBookSource');
  Workers.PostWait(WaitCheckBookSource, FWaitCheckBookSourceSingId);
end;

procedure TForm1.FormDestroy(Sender: TObject);
begin
  FreeAndNil(FBookSrcData);
  FreeAndNil(FBookGroups);
  FreeAndNil(FFilterList);
  FreeAndNil(FBookCopyData);
  FreeAndNil(FLocker);
end;

procedure TForm1.FormShow(Sender: TObject);
begin
  DragAcceptFiles(SrcList.Handle, True);
  DragAcceptFiles(StaticText1.Handle, True);

  OldListWndProc := SrcList.WindowProc;
  OldTextWndProc := StaticText1.WindowProc;
  SrcList.WindowProc := SrcListWndProc;
  StaticText1.WindowProc := SrcListTextWndProc;

  NotifyListChange;
end;

procedure TForm1.G1Click(Sender: TObject);
var
  IsDX: Boolean;
begin
  if FLastSortFlag <> 2 then begin
    FLastSortFlag := 2;
    IsDX := False;
  end else begin
    IsDX := True;
    FLastSortFlag := 0;
  end;
  FBookSrcData.Sort(
    function (A, B: Pointer): Integer
    var
      Item1: PJSONValue absolute A;
      Item2: PJSONValue absolute B;
      S1, S2: string;
    begin
      if (Item1.FType = Item2.FType) and (Item1.FType = jdtObject) and
        (Item1.AsJsonObject <> nil) and (Item2.AsJsonObject <> nil)
      then begin
        S1 := TBookSourceItem(Item1.AsJsonObject).bookSourceGroup;
        S2 := TBookSourceItem(Item2.AsJsonObject).bookSourceGroup;
        if IsDX then
          Result := CompareStr(S2, S1)
        else
          Result := CompareStr(S1, S2);
      end else
        Result := 0;
    end
  );
  NotifyListChange(1);
end;

procedure TForm1.H2Click(Sender: TObject);
var
  FindStr, NewStr: string;
  I, Flag: Integer;
  Item: TBookSourceItem;
begin
  Flag := 0;
  if ShowReplaceGroup(Self, '替换 - 书源分组', FindStr, NewStr, Flag) then begin
    if (FindStr <> '') and (Flag = 0) then    
      Exit;
    for I := 0 to FBookSrcData.Count - 1 do begin
      Item := TBookSourceItem(FBookSrcData.O[I]);
      if not Assigned(Item) then Continue;
      if Flag = 0 then begin              
        Item.bookSourceGroup := StringReplace(Trim(Item.bookSourceGroup), FindStr, NewStr, [rfReplaceAll, rfIgnoreCase]);
      end else begin
        if NewStr = '' then
          Item.RemoveGroup(FindStr)
        else
          Item.ReplaceGroup(FindStr, NewStr);
      end;      
    end;
    NotifyListChange();
  end;   
end;

procedure TForm1.I1Click(Sender: TObject);
var
  Msg: string;
begin
  Msg := Application.Title + sLineBreak + 'YangYxd 版权所有 2019';
  MessageBox(Handle, PChar(Msg), '关于我', 64);
end;

procedure TForm1.Log(const Msg: string);
begin
  LogD(Msg);
  FStateMsg := Msg;
  DispLog();
end;

procedure TForm1.LogD(const Msg: string);
begin
  edtLog.Lines.Add(Format('[%s] %s', [FormatDateTime('hh:mm:ss.zzz', Now), Msg]));
end;

procedure TForm1.N14Click(Sender: TObject);
begin
  FindSource(FLastFindSource);
end;

procedure TForm1.N15Click(Sender: TObject);
var
  I: Integer;
  GName: string;
  Item: TBookSourceItem;
begin
  GName := InputBox('添加分组名称', '输入分组名称', '发现');
  if GName = '' then Exit;

  for I := 0 to SrcList.Count - 1 do begin
    if SrcList.Selected[I] then begin
      Item := TBookSourceItem(JSONObject(FFilterList[I]));
      if not Assigned(Item) then Continue;
      Item.AddGroup(GName);
    end;
  end;
  NotifyListChange;
end;

procedure TForm1.N17Click(Sender: TObject);
var
  I: Integer;
  GName: string;
  Item: TBookSourceItem;
begin
  GName := InputBox('添加分组名称', '输入分组名称', '发现');
  if GName = '' then Exit;

  for I := 0 to SrcList.Count - 1 do begin
    if SrcList.Selected[I] then begin
      Item := TBookSourceItem(JSONObject(FFilterList[I]));
      if not Assigned(Item) then Continue;
      Item.RemoveGroup(GName);
    end;
  end;
  NotifyListChange;
end;

procedure TForm1.N18Click(Sender: TObject);
begin
  ExportSelectedToFile;
end;

procedure TForm1.N7Click(Sender: TObject);
var
  Item: TBookSourceItem;
begin
  Item := TBookSourceItem(JSONObject.Create);
  EditSource(Item);
end;

procedure TForm1.NotifyListChange(Flag: Integer);
var
  I, J: Integer;
  Key: string;
  Item: TBookSourceItem;

  function EqualsBookType(const ABookType: string): Boolean;
  begin
    Result := (FBookType = '') or (FBookType = UpperCase(ABookType)) or
      ((FBookType = 'TEXT') and (ABookType = ''));
  end;

begin
  J := FCurIndex;
  
  if Flag = 0 then begin 
    for I := 0 to FBookSrcData.Count - 1 do
      UpdateBookGroup(TBookSourceItem(FBookSrcData.O[I])); 
    bookGroupList.Items.Clear;
    bookGroupList.Items.Add('发现');
    bookGroupList.Items.Add('失效');
    FBookGroups.GetKeyList(bookGroupList.Items);
  end;   

  FFilterList.Clear;
  Key := LowerCase(bookGroupList.Text);
  if Key <> '' then begin
    for I := 0 to FBookSrcData.Count - 1 do begin
      Item := TBookSourceItem(FBookSrcData.O[I]);
      if Item.bookSourceType = 0 then begin //  EqualsBookType(Item.bookSourceType)
        if (Pos(Key, Item.bookSourceGroup) > 0) or (Pos(Key, Item.bookSourceName) > 0) then
          FFilterList.Add(Item);
      end;
    end;
  end else begin
    for I := 0 to FBookSrcData.Count - 1 do begin
      Item := TBookSourceItem(FBookSrcData.O[I]);
      if Item.bookSourceType = 0 then  // EqualsBookType(Item.bookSourceType)
        FFilterList.Add(Item);
    end;
  end;
  
  SrcList.Count := FFilterList.Count;
  StaticText1.Visible := SrcList.Count = 0;
  if (J < SrcList.Count) and (J >= 0) then begin
    SrcList.ClearSelection;
    SrcList.ItemIndex := J;
    SrcList.Selected[J] := True;
    SrcListClick(SrcList);
  end;
  
  SrcList.ShowHint := SrcList.Count = 0;
  StatusBar1.Panels[0].Text := Format('书源总数：%d个, 当前: %d个', [FBookSrcData.Count, FFilterList.Count]);
end;

procedure TForm1.O1Click(Sender: TObject);
begin
  OpenDialog1.Title := '打开书源文件';
  if OpenDialog1.Execute(Handle) then begin
    FBookSrcData.Clear;
    AddSrcFile(OpenDialog1.FileName);
  end;
end;

procedure TForm1.O2Click(Sender: TObject);
var
  IsDX: Boolean;
begin
  if FLastSortFlag <> 4 then begin
    FLastSortFlag := 4;
    IsDX := False;
  end else begin
    IsDX := True;
    FLastSortFlag := 0;
  end;
  FBookSrcData.Sort(
    function (A, B: Pointer): Integer
    var
      Item1: PJSONValue absolute A;
      Item2: PJSONValue absolute B;
    begin
      if (Item1.FType = Item2.FType) and (Item1.FType = jdtObject) and
        (Item1.AsJsonObject <> nil) and (Item2.AsJsonObject <> nil)
      then begin
        if IsDX then
          Result := TBookSourceItem(Item1.AsJsonObject).score - TBookSourceItem(Item2.AsJsonObject).score
        else
          Result := TBookSourceItem(Item2.AsJsonObject).score - TBookSourceItem(Item1.AsJsonObject).score;
      end else
        Result := 0;
    end
  );
  NotifyListChange(1);
end;

procedure TForm1.P1Click(Sender: TObject);
begin
  EditData.PasteFromClipboard;
end;

procedure TForm1.P2Click(Sender: TObject);
begin
  Paste;
end;

procedure TForm1.Paste;
var
  I: Integer;
begin
  for I := 0 to FBookCopyData.Count - 1 do
    FBookSrcData.AddChildObject.Assign(FBookCopyData.O[I]);
  NotifyListChange();
end;

procedure TForm1.PopupMenu1Popup(Sender: TObject);
begin
  C4.Enabled := SrcList.SelCount > 0;
  X2.Enabled := C4.Enabled;
  C3.Enabled := C4.Enabled;
  E1.Enabled := C4.Enabled;
  D1.Enabled := C4.Enabled;
  P2.Enabled := FBookCopyData.Count > 0;
  N14.Enabled := FLastFindSource <> '';
end;

procedure TForm1.PopupMenu2Popup(Sender: TObject);
begin
  S1.Enabled := SrcList.ItemIndex >= 0;
  P1.Enabled := EditData.CanPaste;
  X1.Enabled := EditData.SelLength > 0;
  C2.Enabled := X1.Enabled;
  R1.Enabled := EditData.CanUndo;
  Z1.Enabled := EditData.CanRedo;
  W1.Checked := EditData.WordWrap;
end;

procedure TForm1.R1Click(Sender: TObject);
begin
  EditData.Undo;
end;

procedure TForm1.R2Click(Sender: TObject);
begin
  ShellExecute(0, 'OPEN', PChar('https://github.com/yangyxd/MyBookshelf'), nil, nil, SW_SHOWMAXIMIZED)
end;

procedure TForm1.RadioButton1Click(Sender: TObject);
begin
  BookType := '';
end;

procedure TForm1.RadioButton2Click(Sender: TObject);
begin
  BookType := 'TEXT';
end;

procedure TForm1.RadioButton3Click(Sender: TObject);
begin
  BookType := 'AUDIO';
end;

procedure TForm1.RadioButton4Click(Sender: TObject);
begin
  BookType := 'IMAGE';
end;

procedure TForm1.RemoveRepeat(AJob: PJob);
var
  CheckName: Boolean;

  function Equals(A, B: TBookSourceItem): Boolean;
  begin
    Result := 
      (LowerCase(A.bookSourceUrl) = LowerCase(B.bookSourceUrl)) and    
      (LowerCase(A.loginUrl) = LowerCase(B.loginUrl)) and    
      (LowerCase(A.header) = LowerCase(B.header)) and
      (LowerCase(A.exploreUrl) = LowerCase(B.exploreUrl)) and
      (LowerCase(A.ruleContent_content) = LowerCase(B.ruleContent_content)) and
      (LowerCase(A.ruleContent_nextContentUrl) = LowerCase(B.ruleContent_nextContentUrl)) and
      (LowerCase(A.ruleSearch_kind) = LowerCase(B.ruleSearch_kind)) and
      (LowerCase(A.ruleSearch_lastChapter) = LowerCase(B.ruleSearch_lastChapter)) and
      (LowerCase(A.ruleSearch_name) = LowerCase(B.ruleSearch_name)) and
      (A.bookSourceType = B.bookSourceType) and
      (LowerCase(A.ruleSearch_bookUrl) = LowerCase(B.ruleSearch_bookUrl)) and
      (LowerCase(A.ruleSearch_coverUrl) = LowerCase(B.ruleSearch_coverUrl)) and
      (LowerCase(A.ruleSearch_intro) = LowerCase(B.ruleSearch_intro)) and
      (LowerCase(A.bookUrlPattern) = LowerCase(B.bookUrlPattern)) and
      (LowerCase(A.ruleToc_chapterName) = LowerCase(B.ruleToc_chapterName)) and
      (LowerCase(A.ruleToc_chapterList) = LowerCase(B.ruleToc_chapterList)) and
      (LowerCase(A.ruleToc_chapterUrl) = LowerCase(B.ruleToc_chapterUrl)) and
      (LowerCase(A.ruleToc_nextTocUrl) = LowerCase(B.ruleToc_nextTocUrl)) and
      (LowerCase(A.ruleContent_webJs) = LowerCase(B.ruleContent_webJs)) and
      (LowerCase(A.ruleBookInfo_author) = LowerCase(B.ruleBookInfo_author)) and
      (LowerCase(A.ruleBookInfo_name) = LowerCase(B.ruleBookInfo_name)) and
      (LowerCase(A.ruleExplore_bookList) = LowerCase(B.ruleExplore_bookList)) and
      (LowerCase(A.ruleExplore_name) = LowerCase(B.ruleExplore_name)) and
      (LowerCase(A.ruleExplore_lastChapter) = LowerCase(B.ruleExplore_lastChapter));
    if not CheckName then
      Result := Result and
        (LowerCase(A.bookSourceName) = LowerCase(B.bookSourceName)) and    
        (LowerCase(A.bookSourceGroup) = LowerCase(B.bookSourceGroup));    
  end;
  
var
  I, J, LastCount, ST: Integer;
  Item: TBookSourceItem;
  T: TProcessState;
  State: PProcessState;
  List: TList;
begin
  I := 0;
  LastCount := FBookSrcData.Count;
  CheckName := Boolean(Integer(AJob.Data));
  
  T.STime := GetTimestamp;
  T.Min := 0;
  T.Value := 0;
  ST := 1000;

  try

    while I < FBookSrcData.Count do begin
      Item := TBookSourceItem(FBookSrcData.O[I]);
      Inc(I);

      if (not FAllSrc) and (FFilterList.IndexOf(Item) < 0) then
        Continue;

      if FAutoDel then begin
        for J := FBookSrcData.Count - 1 downto I do begin
          if Equals(Item, TBookSourceItem(FBookSrcData.O[J])) then
            FBookSrcData.Remove(J);
        end;
      end;

      if FAutoFind then begin
        if Item.exploreUrl <> '' then
          Item.AddGroup('发现')
        else
          Item.RemoveGroup('发现');
      end;

      if AJob.IsTerminated or (FWaitStop > 0) then
        Break;
      if GetTimestamp - T.STime > ST then begin
        ST := 200;
        T.Value := I;
        T.Max := FBookSrcData.Count;
        New(State);
        State^ := T;
        State.NeedFree := True;
        Workers.Post(DoUpdateProcess, State, True);
      end;
    end;

  finally  
    if LastCount <> FBookSrcData.Count then
      Workers.Post(DoNotifyDataChange, nil, True);
    Sleep(100);
    Workers.Post(TaskFinish, nil, True);
  end; 
end;

procedure TForm1.RemoveSelected;
var
  I, V: Integer;
begin
  for I := SrcList.Count - 1 downto 0 do begin
    if SrcList.Selected[I] then begin
      V := FBookSrcData.IndexOfObject(JSONObject(FFilterList[I]));
      if V >= 0 then       
        FBookSrcData.Remove(V);
    end;
  end;
  NotifyListChange;  
end;

procedure TForm1.S1Click(Sender: TObject);
var
  S: string;
  Item: TBookSourceItem;
begin
  if (FCurIndex < 0) or (FCurIndex >= SrcList.Count) then Exit;
  Item := TBookSourceItem(FFilterList[FCurIndex]);
  if not Assigned(Item) then Exit;
  try
    FIsChange := False;
    S := Item.ToString();
    Item.Parse(EditData.Text);
    if not CheckItem(Item) then
      Item.Parse(S);
  finally
    NotifyListChange;
  end;
end;

procedure TForm1.S2Click(Sender: TObject);
var
  IsDX: Boolean;
begin
  if FLastSortFlag <> 1 then begin
    FLastSortFlag := 1;
    IsDX := False;
  end else begin
    IsDX := True;
    FLastSortFlag := 0;
  end;
  FBookSrcData.Sort(
    function (A, B: Pointer): Integer
    var
      Item1: PJSONValue absolute A;
      Item2: PJSONValue absolute B;
      S1, S2: string;
    begin
      if (Item1.FType = Item2.FType) and (Item1.FType = jdtObject) and 
        (Item1.AsJsonObject <> nil) and (Item2.AsJsonObject <> nil) 
      then begin
        S1 := TBookSourceItem(Item1.AsJsonObject).bookSourceName;
        S2 := TBookSourceItem(Item2.AsJsonObject).bookSourceName;
        if IsDX then
          Result := CompareStr(S2, S1)
        else
          Result := CompareStr(S1, S2);
      end else
        Result := 0;
    end
  );
  NotifyListChange(1);
end;

procedure TForm1.S3Click(Sender: TObject);
begin
  ExportSelectedToFile();
end;

procedure TForm1.SetBookType(const Value: string);
begin
  if UpperCase(FBookType) <> UpperCase(Value) then begin
    FBookType := UpperCase(Value);
    NotifyListChange();
  end;
end;

procedure TForm1.SpeedButton1Click(Sender: TObject);
begin
  edtLog.Lines.Clear;
end;

procedure TForm1.SrcListClick(Sender: TObject);
begin
  if SrcList.ItemIndex < 0 then Exit;
  if not CheckSaveState then Exit;
  FCurIndex := SrcList.ItemIndex;
  EditData.Text := TBookSourceItem(FFilterList[FCurIndex]).ToString(4);
  FIsChange := False;
end;

procedure TForm1.SrcListData(Control: TWinControl; Index: Integer;
  var Data: string);
var
  Item: TBookSourceItem;
begin
  if Index < FBookSrcData.Count then begin
    Item := TBookSourceItem(FFilterList[index]);
    Data := Format('【%s】%s', [Item.bookSourceGroup, Item.bookSourceName]);
  end else 
    Data := '';
end;

procedure TForm1.SrcListDblClick(Sender: TObject);
begin
  E1Click(E1);  
end;

procedure TForm1.SrcListKeyDown(Sender: TObject; var Key: Word;
  Shift: TShiftState);

  procedure PasteItems();
  var 
    pGlobal : Thandle;
  begin
    OpenClipboard(Handle);
    try
      pGlobal := GetClipboardData(CF_HDROP); //获取剪贴板的文件数据
      if pGlobal > 0 then
        AddSrcFiles(pGlobal);
    finally
      CloseClipboard;
    end;   
  end;
  
begin
  if Key = VK_DELETE then begin
    RemoveSelected();
  end else if Key = Ord('V') then begin  // 粘贴
    PasteItems();
  end;
end;

procedure TForm1.SrcListTextWndProc(var Message: TMessage);
begin
  if Message.Msg = WM_DROPFILES then
    WMDropFiles(TWMDropFiles(Message))
  else
    OldTextWndProc(Message);
end;

procedure TForm1.SrcListWndProc(var Message: TMessage);
begin
  if Message.Msg = WM_DROPFILES then
    WMDropFiles(TWMDropFiles(Message))
  else
    OldListWndProc(Message);
end;

procedure TForm1.T1Click(Sender: TObject);
var
  Item: TBookSourceItem;
begin
  Item := TBookSourceItem(JSONObject.Create);
  try
    Item.Parse(EditData.Text);
    if CheckBookSourceItem(Item, True, edtLog.Lines) then
      LogD('恭喜, 检测通过!')
    else
      LogD('书源异常!');
    EditData.Text := Item.ToString(4);
  finally
    FreeAndNil(Item);
  end;
end;

procedure TForm1.TaskFinish(AJob: PJob);
var
  I: Integer;
begin
  I := AtomicDecrement(FTaskRef);
  if (I <= 0) or (FWaitStop > 0) then begin
    if (I = 0) and Assigned(Self) and (not (csDestroying in ComponentState)) then begin
      NotifyListChange();
      Button1Click(Button1);
    end;
  end else if not (csDestroying in ComponentState) then begin
    Log('正在校验书源...');       
    Workers.SendSignal(FWaitCheckBookSourceSingId);
  end;
end;

procedure TForm1.Timer1Timer(Sender: TObject);
begin
  DispLog;
end;

procedure TForm1.U1Click(Sender: TObject);
var
  Item: TBookSourceItem;
begin
  Item := TBookSourceItem(JSONObject.Create);
  try
    Item.Parse(EditData.Text);
    Item.lastUpdateTime := GetCurJavaDateTime;
  finally
    FreeAndNil(Item);
  end;
end;

procedure TForm1.UpdateBookGroup(Item: TBookSourceItem);
var
  J: Integer;   
  ARef: Number;
  ABookGroup: TArray<string>;
  AGroup: string;
begin
  ABookGroup := Item.GetGroupList;

  for J := 0 to High(ABookGroup) do begin            
    ARef := 0;
    AGroup := Trim(ABookGroup[J]);
    FBookGroups.TryGetValue(AGroup, ARef);
    Inc(ARef);
    FBookGroups.AddOrUpdate(AGroup, ARef);
  end;
end;

procedure TForm1.URL1Click(Sender: TObject);
begin
  TMenuItem(Sender).Checked := not TMenuItem(Sender).Checked;
end;

procedure TForm1.W1Click(Sender: TObject);
begin
  EditData.WordWrap := not W1.Checked;
end;

procedure TForm1.W2Click(Sender: TObject);
begin
  ShellExecute(0, 'OPEN', PChar('http://www.cnblogs.com/yangyxd/'), nil, nil, SW_SHOWMAXIMIZED)
end;

procedure TForm1.W3Click(Sender: TObject);
var
  IsDX: Boolean;
begin
  if FLastSortFlag <> 3 then begin
    FLastSortFlag := 3;
    IsDX := False;
  end else begin
    IsDX := True;
    FLastSortFlag := 0;
  end;
  FBookSrcData.Sort(
    function (A, B: Pointer): Integer
    var
      Item1: PJSONValue absolute A;
      Item2: PJSONValue absolute B;
    begin
      if (Item1.FType = Item2.FType) and (Item1.FType = jdtObject) and
        (Item1.AsJsonObject <> nil) and (Item2.AsJsonObject <> nil)
      then begin
        if IsDX then
          Result := TBookSourceItem(Item1.AsJsonObject).weight - TBookSourceItem(Item2.AsJsonObject).weight
        else
          Result := TBookSourceItem(Item2.AsJsonObject).weight - TBookSourceItem(Item1.AsJsonObject).weight;
      end else
        Result := 0;
    end
  );
  NotifyListChange(1);
end;

procedure TForm1.WaitCheckBookSource(AJob: PJob);
var
  I, J: Integer;
begin
  if FBookSrcData.Count > 0 then begin
    FCheckCount := FBookSrcData.Count;
    FCurCheckIndex := 0;
    FCheckFinish := 0;
    J := Min(FBookSrcData.Count, FMaxWorkers);
    for I := 0 to J - 1 do begin
      if AJob.IsTerminated then
        Break;
      Workers.Post(DoCheckBookSourceItem, nil);
    end;
  end else
    Workers.Post(TaskFinish, nil, True);
end;

procedure TForm1.WMDropFiles(var Msg: TWMDropFiles);
begin
  AddSrcFiles(Msg.Drop);
end;

procedure TForm1.X1Click(Sender: TObject);
begin
  EditData.CopyToClipboard;
  EditData.SelText := '';
end;

procedure TForm1.X2Click(Sender: TObject);
begin
  CutSelectedt;
end;

procedure TForm1.Z1Click(Sender: TObject);
begin
  EditData.Redo;
end;

end.
