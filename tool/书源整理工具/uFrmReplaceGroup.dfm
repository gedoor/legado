object frmReplaceGroup: TfrmReplaceGroup
  Left = 0
  Top = 0
  BorderStyle = bsDialog
  ClientHeight = 166
  ClientWidth = 345
  Color = clWindow
  Font.Charset = DEFAULT_CHARSET
  Font.Color = clWindowText
  Font.Height = -11
  Font.Name = 'Tahoma'
  Font.Style = []
  OldCreateOrder = False
  Position = poMainFormCenter
  DesignSize = (
    345
    166)
  PixelsPerInch = 96
  TextHeight = 13
  object Label1: TLabel
    Left = 24
    Top = 16
    Width = 60
    Height = 13
    Caption = #26597#25214#20869#23481#65306
  end
  object Label2: TLabel
    Left = 24
    Top = 63
    Width = 60
    Height = 13
    Caption = #26367#25442#20869#23481#65306
  end
  object Edit1: TEdit
    Left = 24
    Top = 35
    Width = 289
    Height = 21
    TabOrder = 0
  end
  object Edit2: TEdit
    Left = 24
    Top = 82
    Width = 289
    Height = 21
    TabOrder = 1
  end
  object CheckBox1: TCheckBox
    Left = 24
    Top = 128
    Width = 97
    Height = 17
    Caption = #20840#23383#21305#37197
    Checked = True
    State = cbChecked
    TabOrder = 2
  end
  object Button1: TButton
    Left = 239
    Top = 125
    Width = 75
    Height = 25
    Anchors = [akRight, akBottom]
    Caption = #30830#23450'(&O)'
    Default = True
    TabOrder = 3
    OnClick = Button1Click
  end
  object Button2: TButton
    Left = 151
    Top = 125
    Width = 75
    Height = 25
    Anchors = [akRight, akBottom]
    Cancel = True
    Caption = #21462#28040'(&C)'
    ModalResult = 2
    TabOrder = 4
  end
end
