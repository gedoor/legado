object Form2: TForm2
  Left = 0
  Top = 0
  BorderStyle = bsNone
  BorderWidth = 1
  Caption = 'Form2'
  ClientHeight = 173
  ClientWidth = 251
  Color = clSilver
  Font.Charset = DEFAULT_CHARSET
  Font.Color = clWindowText
  Font.Height = -11
  Font.Name = 'Tahoma'
  Font.Style = []
  OldCreateOrder = False
  Position = poOwnerFormCenter
  OnClose = FormClose
  PixelsPerInch = 96
  TextHeight = 13
  object Panel1: TPanel
    Left = 0
    Top = 0
    Width = 251
    Height = 173
    Align = alClient
    BevelOuter = bvNone
    Color = clWindow
    ParentBackground = False
    TabOrder = 0
    object Label1: TLabel
      Left = 73
      Top = 88
      Width = 103
      Height = 13
      Alignment = taCenter
      Caption = #27491#22312#22788#29702', '#35831#31561#24453'...'
    end
    object ActivityIndicator1: TActivityIndicator
      AlignWithMargins = True
      Left = 112
      Top = 32
      Animate = True
    end
    object Button1: TButton
      Left = 71
      Top = 120
      Width = 106
      Height = 25
      Cancel = True
      Caption = #21462#28040'(&C)'
      TabOrder = 1
      OnClick = Button1Click
    end
  end
  object Timer1: TTimer
    Enabled = False
    Interval = 100
    OnTimer = Timer1Timer
    Left = 184
    Top = 56
  end
end
