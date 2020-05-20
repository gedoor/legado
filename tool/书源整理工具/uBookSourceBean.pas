unit uBookSourceBean;

interface

uses
  YxdJson, Classes, SysUtils, Math;

type
  TBookSourceItem = class(JSONObject)
  private
    function GetIndexValue(const Index: Integer): string;
    procedure SetIndexValue(const Index: Integer; const Value: string);
    function GetEnable: Boolean;
    function GetSerialNumber: Integer;
    function GetWeight: Integer;
    procedure SetEnable(const Value: Boolean);
    procedure SetSerialNumber(const Value: Integer);
    procedure SetWeight(const Value: Integer);
    function GetLastUpdateTime: Int64;
    procedure SetLastUpdateTime(const Value: Int64);
    function GetScore: Integer;
    procedure SetScore(const Value: Integer);
    function GetEnabledExplore: Boolean;
    procedure SetEnabledExplore(const Value: Boolean);
    function GetBookSourceType: Integer;
    procedure SetBookSourceType(const Value: Integer);
  public
    procedure AddGroup(const Name: string);
    procedure RemoveGroup(const Name: string);
    procedure ReplaceGroup(const Name, NewName: string);
    function GetGroupList(): TArray<string>;

    procedure SetStrValue(const AKey, Value: string);
    function GetStr(const AKey: string): string;

    property bookSourceType: Integer read GetBookSourceType write SetBookSourceType;  // 书源类型
    property bookSourceGroup: string index 0 read GetIndexValue write SetIndexValue;  // 书源分组
    property bookSourceName: string index 1 read GetIndexValue write SetIndexValue;   // 书源名称
    property bookSourceUrl: string index 2 read GetIndexValue write SetIndexValue;    // 书源URL
    property bookUrlPattern: string index 45 read GetIndexValue write SetIndexValue;  // 链接验证
    property header: string index 3 read GetIndexValue write SetIndexValue;           // 请求头
    property loginUrl: string index 4 read GetIndexValue write SetIndexValue;         // 登录URL
    property searchUrl: string index 6 read GetIndexValue write SetIndexValue;        // 搜索地址
    property exploreUrl: string index 7 read GetIndexValue write SetIndexValue;       // 发现地址

    property ruleSearch_bookList: string index 8 read GetIndexValue write SetIndexValue;
    property ruleSearch_name: string index 9 read GetIndexValue write SetIndexValue;
    property ruleSearch_author: string index 10 read GetIndexValue write SetIndexValue;
    property ruleSearch_kind: string index 11 read GetIndexValue write SetIndexValue;
    property ruleSearch_wordCount: string index 12 read GetIndexValue write SetIndexValue;
    property ruleSearch_lastChapter: string index 13 read GetIndexValue write SetIndexValue;
    property ruleSearch_intro: string index 14 read GetIndexValue write SetIndexValue;
    property ruleSearch_coverUrl: string index 15 read GetIndexValue write SetIndexValue;
    property ruleSearch_bookUrl: string index 16 read GetIndexValue write SetIndexValue;

    property ruleExplore_bookList: string index 17 read GetIndexValue write SetIndexValue;
    property ruleExplore_name: string index 18 read GetIndexValue write SetIndexValue;
    property ruleExplore_author: string index 19 read GetIndexValue write SetIndexValue;
    property ruleExplore_kind: string index 20 read GetIndexValue write SetIndexValue;
    property ruleExplore_wordCount: string index 21 read GetIndexValue write SetIndexValue;
    property ruleExplore_lastChapter: string index 22 read GetIndexValue write SetIndexValue;
    property ruleExplore_intro: string index 23 read GetIndexValue write SetIndexValue;
    property ruleExplore_coverUrl: string index 24 read GetIndexValue write SetIndexValue;
    property ruleExplore_bookUrl: string index 25 read GetIndexValue write SetIndexValue;

    property ruleBookInfo_init: string index 26 read GetIndexValue write SetIndexValue;
    property ruleBookInfo_name: string index 27 read GetIndexValue write SetIndexValue;
    property ruleBookInfo_author: string index 28 read GetIndexValue write SetIndexValue;
    property ruleBookInfo_kind: string index 29 read GetIndexValue write SetIndexValue;
    property ruleBookInfo_wordCount: string index 30 read GetIndexValue write SetIndexValue;
    property ruleBookInfo_lastChapter: string index 31 read GetIndexValue write SetIndexValue;
    property ruleBookInfo_intro: string index 32 read GetIndexValue write SetIndexValue;
    property ruleBookInfo_coverUrl: string index 33 read GetIndexValue write SetIndexValue;
    property ruleBookInfo_tocUrl: string index 34 read GetIndexValue write SetIndexValue;

    property ruleToc_chapterList: string index 35 read GetIndexValue write SetIndexValue;
    property ruleToc_chapterName: string index 36 read GetIndexValue write SetIndexValue;
    property ruleToc_chapterUrl: string index 37 read GetIndexValue write SetIndexValue;
    property ruleToc_isVip: string index 38 read GetIndexValue write SetIndexValue;
    property ruleToc_updateTime: string index 39 read GetIndexValue write SetIndexValue;
    property ruleToc_nextTocUrl: string index 40 read GetIndexValue write SetIndexValue;

    property ruleContent_content: string index 41 read GetIndexValue write SetIndexValue;
    property ruleContent_nextContentUrl: string index 42 read GetIndexValue write SetIndexValue;
    property ruleContent_webJs: string index 43 read GetIndexValue write SetIndexValue;
    property ruleContent_sourceRegex: string index 44 read GetIndexValue write SetIndexValue;

    property lastUpdateTime: Int64 read GetLastUpdateTime write SetLastUpdateTime;    // 最后更新时间
    property enable: Boolean read GetEnable write SetEnable;
    property enabledExplore: Boolean read GetEnabledExplore write SetEnabledExplore;
    property customOrder: Integer read GetSerialNumber write SetSerialNumber;
    property weight: Integer read GetWeight write SetWeight;
    property score: Integer read GetScore write SetScore;  // 评分
  end;

implementation

{ TBookSourceItem }

const
  SKeyArray: array [0..45] of string = (
    'bookSourceGroup',
    'bookSourceName',
    'bookSourceUrl',
    'header',
    'loginUrl',
    'bookSourceType',
    'searchUrl',
    'exploreUrl',

    'ruleSearch_bookList',
    'ruleSearch_name',
    'ruleSearch_author',
    'ruleSearch_kind',
    'ruleSearch_wordCount',
    'ruleSearch_lastChapter',
    'ruleSearch_intro',
    'ruleSearch_coverUrl',
    'ruleSearch_bookUrl',

    'ruleExplore_bookList',
    'ruleExplore_name',
    'ruleExplore_author',
    'ruleExplore_kind',
    'ruleExplore_wordCount',
    'ruleExplore_lastChapter',
    'ruleExplore_intro',
    'ruleExplore_coverUrl',
    'ruleExplore_bookUrl',

    'ruleBookInfo_init',
    'ruleBookInfo_name',
    'ruleBookInfo_author',
    'ruleBookInfo_kind',
    'ruleBookInfo_wordCount',
    'ruleBookInfo_lastChapter',
    'ruleBookInfo_intro',
    'ruleBookInfo_coverUrl',
    'ruleBookInfo_tocUrl',

    'ruleToc_chapterList',
    'ruleToc_chapterName',
    'ruleToc_chapterUrl',
    'ruleToc_isVip',
    'ruleToc_updateTime',
    'ruleToc_nextTocUrl',

    'ruleContent_content',
    'ruleContent_nextContentUrl',
    'ruleContent_webJs',
    'ruleContent_sourceRegex',

    'bookUrlPattern'
  );
  SEnabled = 'enable';
  SEnabledExplore = 'enabledExplore';
  SSerialNumber = 'customOrder';
  SWeight = 'weight';

procedure TBookSourceItem.AddGroup(const Name: string);
var
  S: string;
  List: TArray<string>;
  I: Integer;
begin
  S := Trim(bookSourceGroup);
  if S = '' then
    bookSourceGroup := Name
  else begin
    List := GetGroupList();
    for I := Low(List) to High(List) do begin
      if Trim(List[I]) = Name then
        Exit;
    end;
    bookSourceGroup := bookSourceGroup + '; ' + Name;
  end;
end;

function TBookSourceItem.GetBookSourceType: Integer;
begin
  Result := Self.I[SKeyArray[5]];
end;

function TBookSourceItem.GetEnable: Boolean;
begin
  Result := Self.B[SEnabled];
end;

function TBookSourceItem.GetEnabledExplore: Boolean;
begin
  Result := Self.B[SEnabledExplore];
end;

function TBookSourceItem.GetGroupList: TArray<string>;
var
  S: string;
begin
  S := Trim(bookSourceGroup);
  Result := S.Split([',', ';', ':', '，', '；']);
end;

function TBookSourceItem.GetIndexValue(const Index: Integer): string;
begin
  Result := GetStr(SKeyArray[Index]);
end;

function TBookSourceItem.GetLastUpdateTime: Int64;
begin
  Result := Self.I['lastUpdateTime'];
end;

function TBookSourceItem.GetScore: Integer;
begin
  Result := Self.I['score'];
end;

function TBookSourceItem.GetSerialNumber: Integer;
begin
  Result := Self.I[SSerialNumber];
end;

function TBookSourceItem.GetStr(const AKey: string): string;
var
  Key, PKey: string;
  J: Integer;
  Json: JSONObject;
begin
  Key := AKey;
  J := Key.IndexOf('_');
  if J > 0 then begin
    PKey := Key.Substring(0, J);
    Json := Self.O[PKey];
    if Json = nil then
      Result := ''
    else begin
      Key := Key.Substring(J + 1);
      Result := Json.S[Key];
    end;
  end else
    Result := Self.S[Key];
end;

function TBookSourceItem.GetWeight: Integer;
begin
  Result := SElf.I[SWeight];
end;

procedure TBookSourceItem.RemoveGroup(const Name: string);
var
  S: string;
  List: TArray<string>;
  I, J: Integer;
  SB: TStringBuilder;
begin
  S := Trim(bookSourceGroup);
  if S <> '' then begin
    J := 0;
    List := GetGroupList();
    SB := TStringBuilder.Create(Length(bookSourceGroup) * 2);
    for I := Low(List) to High(List) do begin
      if Trim(List[I]) <> Name then begin
        if J > 0 then
          SB.Append('; ');
        SB.Append(Trim(List[I]));
        Inc(J);
      end;
    end;
    bookSourceGroup := SB.ToString;
  end;
end;

procedure TBookSourceItem.ReplaceGroup(const Name, NewName: string);
var
  S: string;
  List: TArray<string>;
  I, J: Integer;
  SB: TStringBuilder;
begin
  S := Trim(bookSourceGroup);
  if S <> '' then begin
    J := 0;
    List := GetGroupList();
    SB := TStringBuilder.Create(Length(bookSourceGroup) * 2);
    for I := Low(List) to High(List) do begin
      if Trim(List[I]) <> Name then begin
        if J > 0 then
          SB.Append('; ');
        SB.Append(Trim(List[I]));
        Inc(J);
      end else if NewName <> '' then begin
        if J > 0 then
          SB.Append('; ');
        SB.Append(Trim(NewName));
        Inc(J);
      end;
    end;
    bookSourceGroup := SB.ToString;
  end;
end;

procedure TBookSourceItem.SetBookSourceType(const Value: Integer);
begin
  Self.I[SKeyArray[5]] := Value;
end;

procedure TBookSourceItem.SetEnable(const Value: Boolean);
begin
  Self.B[SEnabled] := Value;
end;

procedure TBookSourceItem.SetEnabledExplore(const Value: Boolean);
begin
  Self.B[SEnabledExplore] := Value;
end;

procedure TBookSourceItem.SetIndexValue(const Index: Integer;
  const Value: string);
begin
  SetStrValue(SKeyArray[Index], Value);
end;

procedure TBookSourceItem.SetStrValue(const AKey, Value: string);
var
  Key, PKey: string;
  J: Integer;
  Json: JSONObject;
begin
  Key := AKey;
  J := Key.IndexOf('_');
  if J > 0 then begin
    PKey := Key.Substring(0, J);
    Json := Self.O[PKey];
    if Json = nil then
      Json := Self.AddChildObject(PKey);
    Key := Key.Substring(J + 1);
    Json.S[Key] := Value;
  end else
    Self.S[Key] := Value;
end;

procedure TBookSourceItem.SetLastUpdateTime(const Value: Int64);
begin
  Self.I['lastUpdateTime'] := Value;
end;

procedure TBookSourceItem.SetScore(const Value: Integer);
begin
  Self.I['score'] := Value;
end;

procedure TBookSourceItem.SetSerialNumber(const Value: Integer);
begin
  Self.I[SSerialNumber] := Value;
end;

procedure TBookSourceItem.SetWeight(const Value: Integer);
begin
  Self.I[SWeight] := Value;
end;

end.
