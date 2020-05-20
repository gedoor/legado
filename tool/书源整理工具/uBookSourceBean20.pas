unit uBookSourceBean20;

interface

uses
  YxdJson, Classes, SysUtils, Math;

type
  TBookSource20Item = class(JSONObject)
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
  public
    procedure AddGroup(const Name: string);
    procedure RemoveGroup(const Name: string);
    procedure ReplaceGroup(const Name, NewName: string);
    function GetGroupList(): TArray<string>;

    property bookSourceType: string index 28 read GetIndexValue write SetIndexValue;  // 书源类型
    property bookSourceGroup: string index 0 read GetIndexValue write SetIndexValue;  // 书源分组
    property bookSourceName: string index 1 read GetIndexValue write SetIndexValue;   // 书源名称
    property bookSourceUrl: string index 2 read GetIndexValue write SetIndexValue;    // 书源URL
    property httpUserAgent: string index 3 read GetIndexValue write SetIndexValue;    // HttpUserAgent
    property loginUrl: string index 4 read GetIndexValue write SetIndexValue;         // 登录URL
    property ruleBookAuthor: string index 5 read GetIndexValue write SetIndexValue;   // 作者规则
    property ruleBookContent: string index 6 read GetIndexValue write SetIndexValue;  // 正文规则
    property ruleBookKind: string index 7 read GetIndexValue write SetIndexValue;     // 分类规则
    property ruleBookLastChapter: string index 8 read GetIndexValue write SetIndexValue;  // 最新章节规则
    property ruleBookName: string index 9 read GetIndexValue write SetIndexValue;     // 书名规则
    property ruleBookUrlPattern: string index 10 read GetIndexValue write SetIndexValue;  // 书籍详情URL正则
    property ruleChapterList: string index 11 read GetIndexValue write SetIndexValue;   // 目录列表规则
    property ruleChapterName: string index 12 read GetIndexValue write SetIndexValue;   // 章节名称规则
    property ruleChapterUrl: string index 13 read GetIndexValue write SetIndexValue;    // 目录URL规则
    property ruleChapterUrlNext: string index 14 read GetIndexValue write SetIndexValue;  // 目录下一页Url规则
    property ruleContentUrl: string index 15 read GetIndexValue write SetIndexValue;      // 正文章节URL规则
    property ruleContentUrlNext: string index 16 read GetIndexValue write SetIndexValue;  // 正文下一页URL规则
    property ruleCoverUrl: string index 17 read GetIndexValue write SetIndexValue;     // 封面规则
    property ruleFindUrl: string index 18 read GetIndexValue write SetIndexValue;      // 发现规则
    property ruleIntroduce: string index 19 read GetIndexValue write SetIndexValue;      // 简介规则
    property ruleSearchAuthor: string index 20 read GetIndexValue write SetIndexValue;    // 搜索结果作者规则
    property ruleSearchCoverUrl: string index 21 read GetIndexValue write SetIndexValue;    // 搜索结果封面规则
    property ruleSearchKind: string index 22 read GetIndexValue write SetIndexValue;         // 搜索结果分类规则
    property ruleSearchLastChapter: string index 23 read GetIndexValue write SetIndexValue;  // 搜索结果最新章节规则
    property ruleSearchList: string index 24 read GetIndexValue write SetIndexValue;   // 搜索结果列表规则
    property ruleSearchName: string index 25 read GetIndexValue write SetIndexValue;   // 搜索结果书名规则
    property ruleSearchNoteUrl: string index 26 read GetIndexValue write SetIndexValue;  // 搜索结果书籍URL规则
    property ruleSearchUrl: string index 27 read GetIndexValue write SetIndexValue;    // 搜索规地址

    property lastUpdateTime: Int64 read GetLastUpdateTime write SetLastUpdateTime;  // 最后更新时间
    property enable: Boolean read GetEnable write SetEnable;
    property serialNumber: Integer read GetSerialNumber write SetSerialNumber;
    property weight: Integer read GetWeight write SetWeight;
    property score: Integer read GetScore write SetScore;  // 评分
  end;

implementation

{ TBookSourceItem }

const
  SKeyArray: array [0..28] of string = (
    'bookSourceGroup',
    'bookSourceName',
    'bookSourceUrl',
    'httpUserAgent',
    'loginUrl',
    'ruleBookAuthor',
    'ruleBookContent',
    'ruleBookKind',
    'ruleBookLastChapter',
    'ruleBookName',
    'ruleBookUrlPattern',
    'ruleChapterList',
    'ruleChapterName',
    'ruleChapterUrl',
    'ruleChapterUrlNext',
    'ruleContentUrl',
    'ruleContentUrlNext',
    'ruleCoverUrl',
    'ruleFindUrl',
    'ruleIntroduce',
    'ruleSearchAuthor',
    'ruleSearchCoverUrl',
    'ruleSearchKind',
    'ruleSearchLastChapter',
    'ruleSearchList',
    'ruleSearchName',
    'ruleSearchNoteUrl',
    'ruleSearchUrl',
    'bookSourceType'
  );
  SEnabled = 'enable';
  SSerialNumber = 'serialNumber';
  SWeight = 'weight';

procedure TBookSource20Item.AddGroup(const Name: string);
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

function TBookSource20Item.GetEnable: Boolean;
begin
  Result := Self.B[SEnabled];
end;

function TBookSource20Item.GetGroupList: TArray<string>;
var
  S: string;
begin
  S := Trim(bookSourceGroup);
  Result := S.Split([',', ';', ':', '，', '；']);
end;

function TBookSource20Item.GetIndexValue(const Index: Integer): string;
begin
  Result := Self.S[SKeyArray[Index]];
end;

function TBookSource20Item.GetLastUpdateTime: Int64;
begin
  Result := Self.I['lastUpdateTime'];
end;

function TBookSource20Item.GetScore: Integer;
begin
  Result := Self.I['score'];
end;

function TBookSource20Item.GetSerialNumber: Integer;
begin
  Result := Self.I[SSerialNumber];
end;

function TBookSource20Item.GetWeight: Integer;
begin
  Result := SElf.I[SWeight];
end;

procedure TBookSource20Item.RemoveGroup(const Name: string);
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

procedure TBookSource20Item.ReplaceGroup(const Name, NewName: string);
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

procedure TBookSource20Item.SetEnable(const Value: Boolean);
begin
  Self.B[SEnabled] := Value;
end;

procedure TBookSource20Item.SetIndexValue(const Index: Integer;
  const Value: string);
begin
  Self.S[SKeyArray[Index]] := Value;
end;

procedure TBookSource20Item.SetLastUpdateTime(const Value: Int64);
begin
  Self.I['lastUpdateTime'] := Value;
end;

procedure TBookSource20Item.SetScore(const Value: Integer);
begin
  Self.I['score'] := Value;
end;

procedure TBookSource20Item.SetSerialNumber(const Value: Integer);
begin
  Self.I[SSerialNumber] := Value;
end;

procedure TBookSource20Item.SetWeight(const Value: Integer);
begin
  Self.I[SWeight] := Value;
end;

end.
