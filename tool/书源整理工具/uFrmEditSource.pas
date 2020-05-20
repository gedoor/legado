unit uFrmEditSource;

interface

uses
  uBookSourceBean, YxdJson, RegularExpressions,
  Winapi.Windows, Winapi.Messages, System.SysUtils, System.Variants, System.Classes, Vcl.Graphics,
  Vcl.Controls, Vcl.Forms, Vcl.Dialogs, Vcl.StdCtrls, Vcl.ExtCtrls;

type
  TNotifyEventA = reference to procedure (Item: TBookSourceItem);

  TfrmEditSource = class(TForm)
    Panel1: TPanel;
    Button1: TButton;
    Button2: TButton;
    CheckBox1: TCheckBox;
    Label29: TLabel;
    Edit27: TEdit;
    Label30: TLabel;
    Edit28: TEdit;
    Button3: TButton;
    CheckBox2: TCheckBox;
    CheckBox3: TCheckBox;
    ScrollBox1: TScrollBox;
    Panel2: TPanel;
    Label33: TLabel;
    Label6: TLabel;
    Label7: TLabel;
    Label8: TLabel;
    Label9: TLabel;
    Label10: TLabel;
    Label34: TLabel;
    Label11: TLabel;
    Label12: TLabel;
    Label13: TLabel;
    Label20: TLabel;
    Edit18: TEdit;
    Edit11: TEdit;
    Edit10: TEdit;
    Edit9: TEdit;
    Edit30: TEdit;
    Edit8: TEdit;
    Edit7: TEdit;
    Edit6: TEdit;
    Edit5: TEdit;
    Edit4: TEdit;
    Panel3: TPanel;
    Label14: TLabel;
    Label15: TLabel;
    Label16: TLabel;
    Label17: TLabel;
    Label18: TLabel;
    Label19: TLabel;
    Label21: TLabel;
    Label22: TLabel;
    Label23: TLabel;
    Label24: TLabel;
    Label25: TLabel;
    Label2: TLabel;
    Edit12: TEdit;
    Edit13: TEdit;
    Edit14: TEdit;
    Edit15: TEdit;
    Edit16: TEdit;
    Edit17: TEdit;
    Edit19: TEdit;
    Edit20: TEdit;
    Edit21: TEdit;
    Memo1: TMemo;
    Panel4: TPanel;
    Label26: TLabel;
    Label27: TLabel;
    Label35: TLabel;
    Label36: TLabel;
    Label37: TLabel;
    Label38: TLabel;
    Label39: TLabel;
    Label40: TLabel;
    Label41: TLabel;
    Label42: TLabel;
    Edit22: TEdit;
    Edit23: TEdit;
    Edit24: TEdit;
    Edit25: TEdit;
    Edit26: TEdit;
    Edit31: TEdit;
    Edit32: TEdit;
    Edit33: TEdit;
    Edit35: TEdit;
    Panel6: TPanel;
    Label32: TLabel;
    Label5: TLabel;
    Label31: TLabel;
    Label4: TLabel;
    Label3: TLabel;
    Label1: TLabel;
    Memo2: TMemo;
    Edit3: TEdit;
    Edit2: TEdit;
    Edit1: TEdit;
    ComboBox1: TComboBox;
    Edit29: TEdit;
    Panel5: TPanel;
    Label28: TLabel;
    Label43: TLabel;
    Label44: TLabel;
    Label45: TLabel;
    Label46: TLabel;
    Edit41: TEdit;
    Edit42: TEdit;
    Edit43: TEdit;
    Memo3: TMemo;
    Panel7: TPanel;
    Label48: TLabel;
    Label49: TLabel;
    Label50: TLabel;
    Label51: TLabel;
    Label52: TLabel;
    Label53: TLabel;
    Edit34: TEdit;
    Edit36: TEdit;
    Edit37: TEdit;
    Edit38: TEdit;
    Edit44: TEdit;
    CheckBox4: TCheckBox;
    CheckBox5: TCheckBox;
    Label47: TLabel;
    Edit39: TEdit;
    procedure FormShow(Sender: TObject);
    procedure Button1Click(Sender: TObject);
    procedure FormResize(Sender: TObject);
    procedure FormClose(Sender: TObject; var Action: TCloseAction);
    procedure Button2Click(Sender: TObject);
    procedure FormCreate(Sender: TObject);
    procedure Button3Click(Sender: TObject);
    procedure FormMouseWheel(Sender: TObject; Shift: TShiftState;
      WheelDelta: Integer; MousePos: TPoint; var Handled: Boolean);
    procedure CheckBox2Click(Sender: TObject);
    procedure CheckBox3Click(Sender: TObject);
  private
    { Private declarations }
    FDisableChange: Boolean;
  public
    { Public declarations }
    Data: TBookSourceItem;
    CallBack: TNotifyEventA;

    procedure ApplayEdit(Data: TBookSourceItem);
  end;

var
  frmEditSource: TfrmEditSource;

procedure ShowEditSource(Item: TBookSourceItem; CallBack: TNotifyEventA = nil);
function Is20Source(Item: JSONObject): Boolean;
procedure ConvertToNewFormat(Item: TBookSourceItem);

implementation

{$R *.dfm}

uses
  uFrmMain, Math, uBookSourceBean20;

var
  LastW, LastH: Integer;

procedure ShowEditSource(Item: TBookSourceItem; CallBack: TNotifyEventA);
var
  F: TfrmEditSource;
begin
  F := TfrmEditSource.Create(Application);
  F.Data := Item;
  F.CallBack := CallBack;

  if Is20Source(Item) then
    ConvertToNewFormat(F.Data);
    
  F.Show;
end;

function Is20Source(Item: JSONObject): Boolean;
begin
  Result := Item.Contains('ruleSearchUrl') and (not Item.Contains('ruleSearch'))
end;

type
  RegCallback = reference to procedure(const value: string);

procedure ConvertToNewFormat(Item: TBookSourceItem);
var
  Data: TBookSource20Item;
  NewData: TBookSourceItem;
  Tmp: JSONObject;
  S: string;

  function RegMatchs(const pattern, txt: string; callback: RegCallback; all: Boolean = False): string;
  var 
    match : TMatch; 
    groups: TGroupCollection; 
    group: TGroup;
  begin
    match := TRegEx.Match(txt, pattern, [roIgnoreCase]);
    groups := match.Groups;
    if groups.Count = 0 then Exit;    
    if all then begin
      for group in groups do begin
        if Assigned(callback) then
          callback(group.Value);    
      end;
    end else if Assigned(callback) then
      callback(groups[0].Value);
  end;

  function toNewRule(oldRule: string): string;
  var
    reverse: Boolean;
    allinone: Boolean;
    I: Integer;
    list: TArray<string>;
  begin
    if oldRule = '' then begin
      Result := '';
      Exit;
    end;
    reverse := False;
    allinone := False;
    Result := oldRule;
    if (Result.StartsWith('-')) then begin
      reverse := True;
      Result := Result.Substring(1);
    end;
    if Result.StartsWith('+') then begin
      allinone := True;
      Result := Result.Substring(1);
    end;
    if (not Result.StartsWith('@CSS:', True)) and
      (not Result.StartsWith('@XPath:', True)) and
      (not Result.StartsWith('//')) and
      (not Result.StartsWith('##')) and
      (not Result.StartsWith(':')) and
      (not Result.StartsWith('@js:', True)) and
      (not Result.StartsWith('<js>', True))
    then begin
      if Result.Contains('#') and (not Result.Contains('##')) then begin
        Result := Result.Replace('#', '##');
      end;
      if Result.Contains('|') and (not Result.Contains('||')) then begin
        if (Result.Contains('##')) then begin
          list := result.Split(['##']);
          if (list[0].Contains('|')) then begin
            result := list[0].Replace('|', '||');
            for I := 0 to High(list) do
              Result := Result + '##' + list[i];
          end;
        end else begin
          Result := Result.Replace('|', '||');
        end;
      end;

      if Result.Contains('&') and
        (not Result.Contains('&&')) and
        (not Result.Contains('http')) and
        (not Result.Contains('/')) then begin
          result := result.Replace('&', '&&');
        end;

    end;

    if allinone = true then
      Result := '+' + Result;

    if reverse = true then
      Result := '-' + Result;
  end;

  function toNewUrl(oldUrl: string): string;
  var
    url: string;
    list: TArray<string>;
    jsList: TStrings;
    map: JSONObject;
    mather: Boolean;
    I: Integer;
  begin
    if oldUrl = '' then begin
      Result := '';
      Exit;    
    end;
    url := oldUrl;
    if (oldUrl.startsWith('<js>', true)) then begin
      Result := url.replace('=searchKey', '={{key}}')
                  .replace('=searchPage', '={{page}}');
      Exit;
    end;
    map := JSONObject.Create;
    jsList := TStringList.Create;
    try
      RegMatchs('@Header:\{.+?\}', url, 
        procedure (const value: string) begin
          url := url.Replace(value, '');
          map.S['headers'] := value;
        end
      );
    
      list := url.Split(['|']);
      url := list[0];
      if Length(list) > 1 then begin
        list := list[1].Split(['=']);
        if Length(list) > 1 then
          map.S['charset'] := list[1]; 
      end;

      jsList.Clear;
      RegMatchs('\{\{.+?\}\}', url, 
        procedure (const value: string) begin
          jsList.Add(value);
          url := url.Replace(value, '$${' + IntToStr(jsList.Count - 1) + '}');  
        end
      );

      url := url.replace('{', '<').replace('}', '>');
      url := url.replace('searchKey', '{{key}}');
      
      url := TRegEx.Replace(url, '<searchPage([-+]1)>', '{{page$1}}', [roIgnoreCase]);
      url := TRegEx.Replace(url, 'searchPage([-+]1)', '{{page$1}}', [roIgnoreCase]);
      url := url.replace('searchPage', '{{page}}');

      for I := 0 to jsList.Count - 1 do begin
        url := url.replace('$' + IntToStr(i), jsList[i].Replace('searchKey', 'key').Replace('searchPage', 'page'));      
      end;

      list := url.Split(['@']);
      url := list[0];
      if (Length(list) > 1) then begin
        map.S['method'] := 'POST';
        map.S['body'] := list[1];       
      end;

      if map.Count > 0 then
        url := Url + ',' + map.ToString;

      Result := url;
    finally
      FreeAndNil(jsList);
      FreeAndNil(map);
    end;
  end;

  function toNewUrls(const oldUrls: string): string;
  var
    urls: TArray<string>;
    i: Integer;
  begin
    if oldUrls = '' then begin
      Result := '';
      Exit;
    end;
    if (not oldUrls.Contains(#13)) and (not oldUrls.Contains('&&')) then begin
      Result := toNewUrl(oldUrls);
      Exit;
    end;
    Result := '';
    urls := oldurls.Split(['&&', sLineBreak, #13]);
    for I := 0 to High(urls) do begin
      if Trim(urls[i]) = '' then Continue;
      Result := Result + toNewUrl(urls[i]).Replace(sLineBreak, '').Replace(#13, '') + sLineBreak;
    end;       
  end;

  procedure SyncItem(const NewKey, LastKey: string; IsURL: Boolean = False; IsURLs: boolean = false);
  var
    v: string;
  begin
    v := Data.S[LastKey];
    if (v = '') then Exit;
    if IsURL then
      v := toNewUrl(v)
    else if IsURLs then
      v := toNewUrls(v)     
    else
      v := toNewRule(v);
    NewData.SetStrValue(NewKey, v);
  end;

begin
  Tmp := JSONObject.Create();
  try
    Data := TBookSource20Item(Item);
    NewData := TBookSourceItem.Create();
    NewData.enable := data.enable;
    if (data.bookSourceType = 'AUDIO') then
      NewData.bookSourceType := 1
    else if (data.bookSourceType = 'IMAGE') then
      NewData.bookSourceType := 2
    else
      NewData.bookSourceType := 0;
    NewData.bookSourceGroup := data.bookSourceGroup;
    NewData.bookSourceUrl := data.bookSourceUrl;
    NewData.bookUrlPattern := data.ruleBookUrlPattern;
    NewData.bookSourceName := data.bookSourceName;
    NewData.customOrder := data.serialNumber;
    NewData.weight := data.weight;
    NewData.score := data.score;
    NewData.loginUrl := data.loginUrl;
    if (Data.httpUserAgent <> '') then begin
      Tmp.s['User-Agent'] := Data.httpUserAgent;
      NewData.header := tmp.ToString;
    end;
    SyncItem('ruleBookInfo_author', 'ruleBookAuthor');
    SyncItem('searchUrl', 'ruleSearchUrl', True);
    SyncItem('exploreUrl', 'ruleFindUrl', False, True);

    SyncItem('ruleSearch_bookList', 'ruleSearchList');
    SyncItem('ruleSearch_name', 'ruleSearchName');
    SyncItem('ruleSearch_author', 'ruleSearchAuthor');
    SyncItem('ruleSearch_kind', 'ruleSearchKind');
    SyncItem('ruleSearch_intro', 'ruleSearchIntroduce');
    SyncItem('ruleSearch_bookUrl', 'ruleSearchNoteUrl');
    SyncItem('ruleSearch_coverUrl', 'ruleSearchCoverUrl');
    SyncItem('ruleSearch_lastChapter', 'ruleSearchLastChapter');

    SyncItem('ruleExplore_bookList', 'ruleFindList');
    SyncItem('ruleExplore_name', 'ruleFindName');
    SyncItem('ruleExplore_author', 'ruleFindAuthor');
    SyncItem('ruleExplore_intro', 'ruleFindIntroduce');
    SyncItem('ruleExplore_kind', 'ruleFindKind');
    SyncItem('ruleExplore_bookUrl', 'ruleFindNoteUrl');
    SyncItem('ruleExplore_coverUrl', 'ruleFindCoverUrl');
    SyncItem('ruleExplore_lastChapter', 'ruleFindLastChapter');

    SyncItem('ruleBookInfo_init', 'ruleBookInfoInit');
    SyncItem('ruleBookInfo_name', 'ruleBookName');
    SyncItem('ruleBookInfo_author', 'ruleBookAuthor');
    SyncItem('ruleBookInfo_intro', 'ruleIntroduce');
    SyncItem('ruleBookInfo_kind', 'ruleBookKind');
    SyncItem('ruleBookInfo_coverUrl', 'ruleCoverUrl');
    SyncItem('ruleBookInfo_lastChapter', 'ruleBookLastChapter');
    SyncItem('ruleBookInfo_tocUrl', 'ruleChapterUrl');

    SyncItem('ruleToc_chapterList', 'ruleChapterList');
    SyncItem('ruleToc_chapterName', 'ruleChapterName');
    SyncItem('ruleToc_chapterUrl', 'ruleContentUrl');
    SyncItem('ruleToc_nextTocUrl', 'ruleChapterUrlNext');

    S := Data.S['ruleBookContent'];
    if S.StartsWith('$') and (not S.StartsWith('$.')) then
      S := S.Substring(1);
    NewData.SetStrValue('ruleContent_content', S);
    SyncItem('ruleContent_nextContentUrl', 'ruleContentUrlNext');

    Item.Clear;
    Item.Assign(NewData);
  finally
    FreeAndNil(tmp);
  end;
end;

procedure TfrmEditSource.ApplayEdit(Data: TBookSourceItem);

  procedure SetEditValue(Parent: TWinControl);
  var
    I, J: Integer;
    Item: TControl;
    Json: JSONObject;
    Key, PKey, Value: string;
  begin
    for I := 0 to Parent.ControlCount - 1 do begin
      Item := Parent.Controls[I];
      if not Item.Visible then Continue;

      if Item is TPanel then begin
        SetEditValue(TWinControl(Item));
        Continue;
      end;
      
      Key := Item.Hint;
      if Key = '' then Continue;
      
      Json := Data;
      J := Key.IndexOf('_');
      if J > 0 then begin
        PKey := Key.Substring(0, J);
        Json := Data.O[PKey];
        if Json = nil then
          Json := Data.AddChildObject(PKey);
        Key := Key.Substring(J + 1);
      end;

      Value := '';
      if Item is TEdit then
        Value := TEdit(Item).Text
      else if Item is TComboBox then
        Value := TComboBox(Item).Text
      else if Item is TMemo then
        Value := TMemo(Item).Text;

      Json.S[Key] := Value;
      continue;  
      if Value <> '' then
        Json.S[Key] := Value
      else
        Json.Delete(Key);  
    end;
  end;

begin
  Data.enable := CheckBox1.Checked;
  if CheckBox2.Checked then
    Data.bookSourceType := 1
  else if CheckBox3.Checked then
    Data.bookSourceType := 2
  else
    Data.bookSourceType := 0;
  Data.weight := StrToIntDef(Edit27.Text, Data.weight);
  Data.customOrder := StrToIntDef(Edit28.Text, Data.customOrder);
  SetEditValue(ScrollBox1);
end;

procedure TfrmEditSource.Button1Click(Sender: TObject);
begin
  FDisableChange := True;
  try
    ApplayEdit(Data);
  finally
    FDisableChange := False;
  end;

  if Assigned(CallBack) then
    CallBack(Data);
  Close;
end;

procedure TfrmEditSource.Button2Click(Sender: TObject);
begin
  Close;
end;

procedure TfrmEditSource.Button3Click(Sender: TObject);
var
  Item: TBookSourceItem;
  Msg: TStrings;
begin
  Msg := TStringList.Create;
  Item := TBookSourceItem(JSONObject.Create);
  try
    Item.Parse(Data.ToString());
    ApplayEdit(Item);
    if Form1.CheckBookSourceItem(Item, True, Msg) then
      MessageBox(Handle, PChar(Msg.Text), '恭喜, 检测通过!', 64)
    else
      MessageBox(Handle, PChar(Msg.Text), '书源异常', 48)
  finally
    FreeAndNil(Item);
    FreeAndNil(Msg);
  end;
end;

procedure TfrmEditSource.CheckBox2Click(Sender: TObject);
begin
  if CheckBox2.Checked then
    CheckBox3.Checked := False;
end;

procedure TfrmEditSource.CheckBox3Click(Sender: TObject);
begin
  if CheckBox3.Checked then
    CheckBox2.Checked := False;
end;

procedure TfrmEditSource.FormClose(Sender: TObject; var Action: TCloseAction);
begin
  Action := caFree;
end;

procedure TfrmEditSource.FormCreate(Sender: TObject);
begin
  if LastW <= 0 then LastW := Self.Width;
  if LastH <= 0 then LastH := Self.Height;
  Self.SetBounds(Left, Top, LastW, LastH);
end;

procedure TfrmEditSource.FormMouseWheel(Sender: TObject; Shift: TShiftState;
  WheelDelta: Integer; MousePos: TPoint; var Handled: Boolean);
begin
  if WheelDelta < 0 then
    ScrollBox1.Perform(WM_VSCROLL,SB_LINEDOWN,0)
  else
    ScrollBox1.Perform(WM_VSCROLL,SB_LINEUP,0);
end;

procedure TfrmEditSource.FormResize(Sender: TObject);
begin
  LastW := Width;
  LastH := Height;
end;

procedure TfrmEditSource.FormShow(Sender: TObject);

  procedure SetEditValue(Parent: TWinControl);
  var
    I, J: Integer;
    Item: TControl;
    Json: JSONObject;
    Key, PKey, Value: string;
  begin
    for I := 0 to Parent.ControlCount - 1 do begin
      Item := Parent.Controls[I];
      if not Item.Visible then Continue;

      if Item is TPanel then begin
        SetEditValue(TWinControl(Item));
        Continue;
      end;
      
      Key := Item.Hint;
      if Key = '' then Continue;

      Value := '';            
      Json := Data;
      J := Key.IndexOf('_');
      if J > 0 then begin
        PKey := Key.Substring(0, J);
        Json := Data.O[PKey];
        if Json <> nil then begin
          Key := Key.Substring(J + 1);
          Value := Json.S[Key];
        end;
      end else
        Value := Json.S[Key];

      
      if Item is TEdit then
        TEdit(Item).Text := Value
      else if Item is TComboBox then
        TComboBox(Item).Text := Value
      else if Item is TMemo then
        TMemo(Item).Text := Value
    end;
  end;

begin
  ComboBox1.Items := Form1.bookGroupList.Items;

  if Assigned(Data) then begin
    FDisableChange := True;
    try
      CheckBox1.Checked := Data.enable;
      CheckBox2.Checked := False;
      CheckBox3.Checked := False;

      if Data.bookSourceType = 1 then
        CheckBox2.Checked := True;
      if Data.bookSourceType = 2 then
        CheckBox3.Checked := True;

      Edit27.Text := IntToStr(Data.weight);
      Edit28.Text := IntToStr(Data.customOrder);
      SetEditValue(ScrollBox1);
      
    finally
      FDisableChange := False;
    end;
  end;
end;

end.
