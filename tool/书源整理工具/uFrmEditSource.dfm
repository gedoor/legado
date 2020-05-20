object frmEditSource: TfrmEditSource
  Left = 0
  Top = 0
  BorderIcons = [biSystemMenu]
  Caption = #32534#36753#20070#28304
  ClientHeight = 743
  ClientWidth = 1198
  Color = clBtnFace
  Font.Charset = DEFAULT_CHARSET
  Font.Color = clWindowText
  Font.Height = -12
  Font.Name = 'Courier New'
  Font.Style = []
  OldCreateOrder = False
  Position = poScreenCenter
  OnClose = FormClose
  OnCreate = FormCreate
  OnMouseWheel = FormMouseWheel
  OnResize = FormResize
  OnShow = FormShow
  PixelsPerInch = 96
  TextHeight = 15
  object Panel1: TPanel
    Left = 0
    Top = 700
    Width = 1198
    Height = 43
    Align = alBottom
    BevelOuter = bvNone
    Font.Charset = DEFAULT_CHARSET
    Font.Color = clWindowText
    Font.Height = -12
    Font.Name = #23435#20307
    Font.Style = []
    ParentFont = False
    ShowCaption = False
    TabOrder = 0
    DesignSize = (
      1198
      43)
    object Label29: TLabel
      Left = 413
      Top = 19
      Width = 60
      Height = 12
      Caption = #25628#32034#26435#37325#65306
      Font.Charset = DEFAULT_CHARSET
      Font.Color = clTeal
      Font.Height = -12
      Font.Name = #23435#20307
      Font.Style = []
      ParentFont = False
    end
    object Label30: TLabel
      Left = 564
      Top = 19
      Width = 60
      Height = 12
      Caption = #25490#24207#32534#21495#65306
      Font.Charset = DEFAULT_CHARSET
      Font.Color = clTeal
      Font.Height = -12
      Font.Name = #23435#20307
      Font.Style = []
      ParentFont = False
    end
    object Button1: TButton
      Left = 1098
      Top = 10
      Width = 88
      Height = 25
      Anchors = [akTop, akRight]
      Caption = #30830#23450'(&O)'
      Default = True
      TabOrder = 0
      OnClick = Button1Click
    end
    object Button2: TButton
      Left = 995
      Top = 10
      Width = 88
      Height = 25
      Anchors = [akTop, akRight]
      Cancel = True
      Caption = #21462#28040'(&C)'
      ModalResult = 2
      TabOrder = 1
      OnClick = Button2Click
    end
    object CheckBox1: TCheckBox
      Left = 12
      Top = 16
      Width = 113
      Height = 17
      Caption = #21551#29992#20070#28304
      TabOrder = 2
    end
    object Edit27: TEdit
      Left = 477
      Top = 14
      Width = 76
      Height = 20
      Hint = 'weight'
      NumbersOnly = True
      ParentShowHint = False
      ShowHint = True
      TabOrder = 3
      Text = '0'
    end
    object Edit28: TEdit
      Left = 627
      Top = 14
      Width = 87
      Height = 20
      Hint = 'weight'
      NumbersOnly = True
      ParentShowHint = False
      ShowHint = True
      TabOrder = 4
      Text = '0'
    end
    object Button3: TButton
      Left = 793
      Top = 10
      Width = 107
      Height = 25
      Anchors = [akTop, akRight]
      Caption = #27979#35797#20070#28304'(&T)'
      ModalResult = 2
      TabOrder = 5
      OnClick = Button3Click
    end
    object CheckBox2: TCheckBox
      Left = 268
      Top = 16
      Width = 80
      Height = 17
      Caption = #38899#39057#28304
      TabOrder = 6
      OnClick = CheckBox2Click
    end
    object CheckBox3: TCheckBox
      Left = 333
      Top = 16
      Width = 80
      Height = 17
      Caption = #22270#28304
      TabOrder = 7
      OnClick = CheckBox3Click
    end
    object CheckBox4: TCheckBox
      Left = 90
      Top = 12
      Width = 87
      Height = 25
      Caption = #21551#29992#21457#29616
      TabOrder = 8
    end
    object CheckBox5: TCheckBox
      Left = 169
      Top = 12
      Width = 87
      Height = 25
      Caption = #21551#29992#25628#32034
      TabOrder = 9
    end
  end
  object ScrollBox1: TScrollBox
    AlignWithMargins = True
    Left = 3
    Top = 3
    Width = 1192
    Height = 694
    HorzScrollBar.Smooth = True
    HorzScrollBar.Style = ssFlat
    VertScrollBar.Smooth = True
    VertScrollBar.Style = ssFlat
    VertScrollBar.Tracking = True
    Align = alClient
    BevelInner = bvNone
    BevelOuter = bvNone
    BorderStyle = bsNone
    Color = clWindow
    Padding.Bottom = 8
    ParentColor = False
    TabOrder = 1
    object Panel2: TPanel
      Left = 0
      Top = 175
      Width = 1175
      Height = 295
      Hint = 'ruleSearch'
      Align = alTop
      BevelOuter = bvNone
      Caption = 'Panel2'
      ShowCaption = False
      TabOrder = 1
      DesignSize = (
        1175
        295)
      object Label33: TLabel
        Left = 9
        Top = 7
        Width = 151
        Height = 16
        Caption = #25628#32034' - ruleSearch'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clBlack
        Font.Height = -16
        Font.Name = #40657#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label6: TLabel
        Left = 24
        Top = 38
        Width = 142
        Height = 12
        Caption = #25628#32034#22320#22336#65306'(searchUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label7: TLabel
        Left = 24
        Top = 64
        Width = 135
        Height = 12
        Caption = #21015#34920#35268#21017#65306'(bookList)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label8: TLabel
        Left = 24
        Top = 89
        Width = 107
        Height = 12
        Caption = #20070#21517#35268#21017#65306'(name)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label9: TLabel
        Left = 24
        Top = 115
        Width = 121
        Height = 12
        Caption = #20316#32773#35268#21017#65306'(author)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label10: TLabel
        Left = 24
        Top = 140
        Width = 107
        Height = 12
        Caption = #20998#31867#35268#21017#65306'(kind)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label34: TLabel
        Left = 24
        Top = 165
        Width = 142
        Height = 12
        Caption = #23383#25968#35268#21017#65306'(wordCount)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label11: TLabel
        Left = 24
        Top = 190
        Width = 162
        Height = 12
        Caption = #26368#26032#31456#33410#35268#21017#65306'(lastChapter)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = []
        ParentFont = False
      end
      object Label12: TLabel
        Left = 24
        Top = 215
        Width = 135
        Height = 12
        Caption = #23553#38754#35268#21017#65306'(coverUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label13: TLabel
        Left = 24
        Top = 241
        Width = 128
        Height = 12
        Caption = #35814#24773#22320#22336#65306'(bookUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label20: TLabel
        Left = 24
        Top = 267
        Width = 114
        Height = 12
        Caption = #31616#20171#35268#21017#65306'(intro)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clWindowText
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Edit18: TEdit
        Left = 280
        Top = 236
        Width = 890
        Height = 23
        Hint = 'ruleSearch_bookUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 8
        TextHint = #36873#25321#33410#28857#20070#31821#35814#24773#39029#32593#22336
      end
      object Edit11: TEdit
        Left = 280
        Top = 262
        Width = 890
        Height = 23
        Hint = 'ruleSearch_intro'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 9
        TextHint = #36873#25321#20070#31821#31616#20171
      end
      object Edit10: TEdit
        Left = 280
        Top = 210
        Width = 890
        Height = 23
        Hint = 'ruleSearch_coverUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 7
        TextHint = #36873#25321#33410#28857#20070#31821#23553#38754
      end
      object Edit9: TEdit
        Left = 280
        Top = 185
        Width = 890
        Height = 23
        Hint = 'ruleSearch_lastChapter'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 6
        TextHint = #36873#25321#33410#28857#26368#26032#31456#33410
      end
      object Edit30: TEdit
        Left = 280
        Top = 160
        Width = 890
        Height = 23
        Hint = 'ruleSearch_wordCount'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 5
        TextHint = #36873#25321#33410#28857#23383#25968#20449#24687
      end
      object Edit8: TEdit
        Left = 280
        Top = 135
        Width = 890
        Height = 23
        Hint = 'ruleSearch_kind'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 4
        TextHint = #36873#25321#33410#28857#20998#31867#20449#24687
      end
      object Edit7: TEdit
        Left = 280
        Top = 110
        Width = 890
        Height = 23
        Hint = 'ruleSearch_author'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 3
        TextHint = #36873#25321#33410#28857#20316#32773
      end
      object Edit6: TEdit
        Left = 280
        Top = 85
        Width = 890
        Height = 23
        Hint = 'ruleSearch_name'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 2
        TextHint = #36873#25321#33410#28857#20070#21517
      end
      object Edit5: TEdit
        Left = 280
        Top = 59
        Width = 890
        Height = 23
        Hint = 'ruleSearch_bookList'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 1
        TextHint = #36873#25321#20070#31821#33410#28857
      end
      object Edit4: TEdit
        Left = 280
        Top = 33
        Width = 890
        Height = 23
        Hint = 'searchUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 0
        TextHint = '['#22495#21517#21487#30465#30053'] /search.php@kw={{key}}'
      end
    end
    object Panel3: TPanel
      Left = 0
      Top = 470
      Width = 1175
      Height = 385
      Hint = 'ruleExplore'
      Align = alTop
      BevelOuter = bvNone
      Caption = 'Panel2'
      ShowCaption = False
      TabOrder = 2
      DesignSize = (
        1175
        385)
      object Label14: TLabel
        Left = 9
        Top = 7
        Width = 160
        Height = 16
        Caption = #21457#29616' - ruleExplore'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clBlack
        Font.Height = -16
        Font.Name = #40657#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label15: TLabel
        Left = 24
        Top = 38
        Width = 149
        Height = 12
        Caption = #21457#29616#22320#22336#65306'(exploreUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label16: TLabel
        Left = 24
        Top = 156
        Width = 135
        Height = 12
        Caption = #21015#34920#35268#21017#65306'(bookList)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label17: TLabel
        Left = 24
        Top = 181
        Width = 107
        Height = 12
        Caption = #20070#21517#35268#21017#65306'(name)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label18: TLabel
        Left = 24
        Top = 207
        Width = 121
        Height = 12
        Caption = #20316#32773#35268#21017#65306'(author)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label19: TLabel
        Left = 24
        Top = 232
        Width = 107
        Height = 12
        Caption = #20998#31867#35268#21017#65306'(kind)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label21: TLabel
        Left = 24
        Top = 257
        Width = 142
        Height = 12
        Caption = #23383#25968#35268#21017#65306'(wordCount)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label22: TLabel
        Left = 24
        Top = 282
        Width = 162
        Height = 12
        Caption = #26368#26032#31456#33410#35268#21017#65306'(lastChapter)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = []
        ParentFont = False
      end
      object Label23: TLabel
        Left = 24
        Top = 307
        Width = 135
        Height = 12
        Caption = #23553#38754#35268#21017#65306'(coverUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label24: TLabel
        Left = 24
        Top = 333
        Width = 128
        Height = 12
        Caption = #35814#24773#22320#22336#65306'(bookUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label25: TLabel
        Left = 24
        Top = 359
        Width = 114
        Height = 12
        Caption = #31616#20171#35268#21017#65306'(intro)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clWindowText
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label2: TLabel
        Left = 32
        Top = 70
        Width = 240
        Height = 60
        Caption = 
          #20869#23481#33021#26174#31034#22312#21457#29616#33756#21333#13#10#27599#34892#19968#26465#21457#29616#20998#31867#65288#32593#22336#22495#21517#21487#30465#30053#65289#65292#20363#65306#13#10#21517#31216'1::'#32593#22336'(Url)1'#13#10#21517#31216'2::'#32593#22336'(Url)2'#13#10 +
          '...'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGray
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = []
        ParentFont = False
      end
      object Edit12: TEdit
        Left = 280
        Top = 328
        Width = 890
        Height = 23
        Hint = 'ruleExplore_bookUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 8
        TextHint = #36873#25321#33410#28857#20070#31821#35814#24773#39029#32593#22336
      end
      object Edit13: TEdit
        Left = 280
        Top = 354
        Width = 890
        Height = 23
        Hint = 'ruleExplore_intro'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 9
        TextHint = #36873#25321#20070#31821#31616#20171
      end
      object Edit14: TEdit
        Left = 280
        Top = 302
        Width = 890
        Height = 23
        Hint = 'ruleExplore_coverUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 7
        TextHint = #36873#25321#33410#28857#20070#31821#23553#38754
      end
      object Edit15: TEdit
        Left = 280
        Top = 277
        Width = 890
        Height = 23
        Hint = 'ruleExplore_lastChapter'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 6
        TextHint = #36873#25321#33410#28857#26368#26032#31456#33410
      end
      object Edit16: TEdit
        Left = 280
        Top = 252
        Width = 890
        Height = 23
        Hint = 'ruleExplore_wordCount'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 5
        TextHint = #36873#25321#33410#28857#23383#25968#20449#24687
      end
      object Edit17: TEdit
        Left = 280
        Top = 227
        Width = 890
        Height = 23
        Hint = 'ruleExplore_kind'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 4
        TextHint = #36873#25321#33410#28857#20998#31867#20449#24687
      end
      object Edit19: TEdit
        Left = 280
        Top = 202
        Width = 890
        Height = 23
        Hint = 'ruleExplore_author'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 3
        TextHint = #36873#25321#33410#28857#20316#32773
      end
      object Edit20: TEdit
        Left = 280
        Top = 176
        Width = 890
        Height = 23
        Hint = 'ruleExplore_name'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 2
        TextHint = #36873#25321#33410#28857#20070#21517
      end
      object Edit21: TEdit
        Left = 280
        Top = 151
        Width = 890
        Height = 23
        Hint = 'ruleExplore_bookList'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 1
        TextHint = #36873#25321#20070#31821#33410#28857
      end
      object Memo1: TMemo
        Left = 280
        Top = 33
        Width = 890
        Height = 115
        Hint = 'exploreUrl'
        Margins.Bottom = 8
        Anchors = [akLeft, akTop, akRight]
        ScrollBars = ssVertical
        TabOrder = 0
      end
    end
    object Panel4: TPanel
      Left = 0
      Top = 855
      Width = 1175
      Height = 270
      Hint = 'ruleSearch'
      Align = alTop
      BevelOuter = bvNone
      Caption = 'Panel2'
      ShowCaption = False
      TabOrder = 3
      DesignSize = (
        1175
        270)
      object Label26: TLabel
        Left = 9
        Top = 7
        Width = 169
        Height = 16
        Caption = #35814#24773' - ruleBookInfo'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clBlack
        Font.Height = -16
        Font.Name = #40657#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label27: TLabel
        Left = 24
        Top = 38
        Width = 120
        Height = 12
        Caption = #39044#22788#29702#35268#21017#65306'(init)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label35: TLabel
        Left = 24
        Top = 64
        Width = 107
        Height = 12
        Caption = #20070#21517#35268#21017#65306'(name)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label36: TLabel
        Left = 24
        Top = 90
        Width = 121
        Height = 12
        Caption = #20316#32773#35268#21017#65306'(author)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label37: TLabel
        Left = 24
        Top = 115
        Width = 107
        Height = 12
        Caption = #20998#31867#35268#21017#65306'(kind)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label38: TLabel
        Left = 24
        Top = 140
        Width = 142
        Height = 12
        Caption = #23383#25968#35268#21017#65306'(wordCount)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label39: TLabel
        Left = 24
        Top = 165
        Width = 162
        Height = 12
        Caption = #26368#26032#31456#33410#35268#21017#65306'(lastChapter)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = []
        ParentFont = False
      end
      object Label40: TLabel
        Left = 24
        Top = 190
        Width = 135
        Height = 12
        Caption = #23553#38754#35268#21017#65306'(coverUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label41: TLabel
        Left = 24
        Top = 216
        Width = 121
        Height = 12
        Caption = #30446#24405#22320#22336#65306'(tocUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label42: TLabel
        Left = 24
        Top = 242
        Width = 114
        Height = 12
        Caption = #31616#20171#35268#21017#65306'(intro)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clWindowText
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Edit22: TEdit
        Left = 280
        Top = 211
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_tocUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 7
        TextHint = #36873#25321#33410#28857#20070#31821#30446#24405#21015#34920#22320#22336
      end
      object Edit23: TEdit
        Left = 280
        Top = 237
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_intro'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 8
        TextHint = #36873#25321#20070#31821#31616#20171
      end
      object Edit24: TEdit
        Left = 280
        Top = 185
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_coverUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 6
        TextHint = #36873#25321#33410#28857#20070#31821#23553#38754
      end
      object Edit25: TEdit
        Left = 280
        Top = 160
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_lastChapter'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 5
        TextHint = #36873#25321#33410#28857#26368#26032#31456#33410
      end
      object Edit26: TEdit
        Left = 280
        Top = 135
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_wordCount'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 4
        TextHint = #36873#25321#33410#28857#23383#25968#20449#24687
      end
      object Edit31: TEdit
        Left = 280
        Top = 110
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_kind'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 3
        TextHint = #36873#25321#33410#28857#20998#31867#20449#24687
      end
      object Edit32: TEdit
        Left = 280
        Top = 85
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_author'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 2
        TextHint = #36873#25321#33410#28857#20316#32773
      end
      object Edit33: TEdit
        Left = 280
        Top = 59
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_name'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 1
        TextHint = #36873#25321#33410#28857#20070#21517
      end
      object Edit35: TEdit
        Left = 280
        Top = 33
        Width = 890
        Height = 23
        Hint = 'ruleBookInfo_init'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 0
        TextHint = #29992#20110#21152#36895#35814#24773#20449#24687#26816#32034#65292#21482#25903#25345'AllInOne'#35268#21017
      end
    end
    object Panel6: TPanel
      Left = 0
      Top = 0
      Width = 1175
      Height = 175
      Align = alTop
      BevelOuter = bvNone
      ShowCaption = False
      TabOrder = 0
      DesignSize = (
        1175
        175)
      object Label32: TLabel
        Left = 8
        Top = 121
        Width = 222
        Height = 12
        Caption = #35831#27714#22836#65306'(header) '#65288#33258#23450#20041#23458#25143#31471#26631#35782#65289
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clTeal
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = []
        ParentFont = False
      end
      object Label5: TLabel
        Left = 8
        Top = 94
        Width = 114
        Height = 12
        Caption = #30331#24405'URL'#65306'(loginUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clTeal
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = []
        ParentFont = False
      end
      object Label31: TLabel
        Left = 8
        Top = 67
        Width = 174
        Height = 12
        Caption = #20070#31821'URL'#27491#21017#65306'(bookUrlPattern)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clTeal
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = []
        ParentFont = False
      end
      object Label4: TLabel
        Left = 8
        Top = 40
        Width = 165
        Height = 12
        Caption = #20070#28304'URL'#65306'(bookSourceUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clRed
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label3: TLabel
        Left = 8
        Top = 13
        Width = 177
        Height = 12
        Caption = #20070#28304#21517#31216#65306'(bookSourceName)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clRed
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label1: TLabel
        Left = 588
        Top = 13
        Width = 184
        Height = 12
        Caption = #20998#32452#21517#31216#65306'(bookSourceGroup)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clWindowText
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Memo2: TMemo
        Left = 280
        Top = 116
        Width = 890
        Height = 53
        Hint = 'header'
        Margins.Bottom = 8
        Anchors = [akLeft, akTop, akRight]
        ParentShowHint = False
        ScrollBars = ssVertical
        ShowHint = False
        TabOrder = 4
      end
      object Edit3: TEdit
        Left = 280
        Top = 89
        Width = 890
        Height = 23
        Hint = 'loginUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 3
        TextHint = #22635#20889#32593#31449#30331#24405#32593#22336#65292#20165#22312#38656#35201#30331#24405#30340#20070#28304#26377#29992
      end
      object Edit2: TEdit
        Left = 280
        Top = 35
        Width = 890
        Height = 23
        Hint = 'bookSourceUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 1
        TextHint = #65288#24517#22635#65289#36890#24120#22635#20889#32593#31449#20027#39029
      end
      object Edit1: TEdit
        Left = 280
        Top = 8
        Width = 290
        Height = 23
        Hint = 'bookSourceName'
        TabOrder = 0
        TextHint = #65288#24517#22635#65289#26174#31034#22312#20070#28304#21015#34920
      end
      object ComboBox1: TComboBox
        Left = 778
        Top = 8
        Width = 392
        Height = 23
        Hint = 'bookSourceGroup'
        Anchors = [akLeft, akTop, akRight]
        DropDownCount = 20
        TabOrder = 5
      end
      object Edit29: TEdit
        Left = 280
        Top = 62
        Width = 890
        Height = 23
        Hint = 'bookUrlPattern'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 2
        TextHint = #24403#35814#24773#39029'URL'#19982#28304'URL'#30340#22495#21517#19981#19968#33268#26102#26377#25928#65292#29992#20110#28155#21152#32593#22336
      end
    end
    object Panel5: TPanel
      Left = 0
      Top = 1315
      Width = 1175
      Height = 195
      Hint = 'ruleSearch'
      Align = alTop
      BevelOuter = bvNone
      Caption = 'Panel2'
      ShowCaption = False
      TabOrder = 5
      DesignSize = (
        1175
        195)
      object Label28: TLabel
        Left = 9
        Top = 7
        Width = 160
        Height = 16
        Caption = #27491#25991' - ruleContent'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clBlack
        Font.Height = -16
        Font.Name = #40657#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label43: TLabel
        Left = 24
        Top = 38
        Width = 128
        Height = 12
        Caption = #27491#25991#35268#21017#65306'(content)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label44: TLabel
        Left = 24
        Top = 64
        Width = 177
        Height = 12
        Caption = #32763#39029#35268#21017#65306'(nextContentUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label45: TLabel
        Left = 24
        Top = 90
        Width = 156
        Height = 12
        Caption = #36164#28304#27491#21017#65306'(sourceRegex)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label46: TLabel
        Left = 24
        Top = 115
        Width = 114
        Height = 12
        Caption = #33050#26412#27880#20837#65306'(webJs)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Edit41: TEdit
        Left = 280
        Top = 85
        Width = 890
        Height = 23
        Hint = 'ruleContent_sourceRegex'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 2
        TextHint = #21305#37197#36164#28304#30340'URL'#29305#24449#65292#29992#20110#21957#25506
      end
      object Edit42: TEdit
        Left = 280
        Top = 59
        Width = 890
        Height = 23
        Hint = 'ruleContent_nextContentUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 1
        TextHint = #36873#25321#19979#19968#20998#39029#65288#19981#26159#19979#19968#31456#65289#38142#25509#65288#35268#21017#32467#26524#20026'String'#31867#22411#30340'Url)'
      end
      object Edit43: TEdit
        Left = 280
        Top = 33
        Width = 890
        Height = 23
        Hint = 'ruleContent_content'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 0
        TextHint = #36873#25321#27491#25991#20869#23481
      end
      object Memo3: TMemo
        Left = 280
        Top = 112
        Width = 890
        Height = 53
        Hint = 'ruleContent_webJs'
        Margins.Bottom = 8
        Anchors = [akLeft, akTop, akRight]
        ParentShowHint = False
        ScrollBars = ssVertical
        ShowHint = False
        TabOrder = 3
      end
    end
    object Panel7: TPanel
      Left = 0
      Top = 1125
      Width = 1175
      Height = 190
      Hint = 'ruleSearch'
      Align = alTop
      BevelOuter = bvNone
      Caption = 'Panel2'
      ShowCaption = False
      TabOrder = 4
      DesignSize = (
        1175
        190)
      object Label48: TLabel
        Left = 9
        Top = 7
        Width = 124
        Height = 16
        Caption = #30446#24405' - ruleToc'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clBlack
        Font.Height = -16
        Font.Name = #40657#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label49: TLabel
        Left = 24
        Top = 38
        Width = 156
        Height = 12
        Caption = #21015#34920#35268#21017#65306'(chapterList)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label50: TLabel
        Left = 24
        Top = 64
        Width = 156
        Height = 12
        Caption = #31456#33410#21517#31216#65306'(chapterName)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label51: TLabel
        Left = 24
        Top = 90
        Width = 149
        Height = 12
        Caption = #31456#33410#22320#22336#65306'(chapterUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label52: TLabel
        Left = 24
        Top = 115
        Width = 114
        Height = 12
        Caption = #25910#36153#26631#35782#65306'(isVip)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label53: TLabel
        Left = 24
        Top = 140
        Width = 149
        Height = 12
        Caption = #32763#39029#35268#21017#65306'(nextTocUrl)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Label47: TLabel
        Left = 24
        Top = 166
        Width = 149
        Height = 12
        Caption = #31456#33410#20449#24687#65306'(updateTime)'
        Font.Charset = DEFAULT_CHARSET
        Font.Color = clGreen
        Font.Height = -12
        Font.Name = #23435#20307
        Font.Style = [fsBold]
        ParentFont = False
      end
      object Edit34: TEdit
        Left = 280
        Top = 135
        Width = 890
        Height = 23
        Hint = 'ruleToc_nextTocUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 4
        TextHint = #36873#25321#30446#24405#19979#19968#39029#30340#38142#25509#65288#35268#21017#32467#26524#20026' List<String> '#25110' String'#65289
      end
      object Edit36: TEdit
        Left = 280
        Top = 110
        Width = 890
        Height = 23
        Hint = 'ruleToc_isVip'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 3
        TextHint = #31456#33410#26159#21542#20026'VIP'#31456#33410#65288#35268#21017#32467#26524#20026'Bool'#65289
      end
      object Edit37: TEdit
        Left = 280
        Top = 84
        Width = 890
        Height = 23
        Hint = 'ruleToc_chapterUrl'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 2
        TextHint = #36873#25321#31456#33410#38142#25509
      end
      object Edit38: TEdit
        Left = 280
        Top = 59
        Width = 890
        Height = 23
        Hint = 'ruleToc_chapterName'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 1
        TextHint = #36873#25321#31456#33410#21517#31216
      end
      object Edit44: TEdit
        Left = 280
        Top = 33
        Width = 890
        Height = 23
        Hint = 'ruleToc_chapterList'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 0
        TextHint = #36873#25321#30446#24405#21015#34920#30340#31456#33410#33410#28857#65288#35268#21017#32467#26524#20026'List<Element>'#65289
      end
      object Edit39: TEdit
        Left = 280
        Top = 161
        Width = 890
        Height = 23
        Hint = 'ruleToc_updateTime'
        Anchors = [akLeft, akTop, akRight]
        TabOrder = 5
        TextHint = #36873#25321#31456#33410#20449#24687
      end
    end
  end
end
