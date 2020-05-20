unit uFrmReplaceGroup;

interface

uses
  Winapi.Windows, Winapi.Messages, System.SysUtils, System.Variants, System.Classes, Vcl.Graphics,
  Vcl.Controls, Vcl.Forms, Vcl.Dialogs, Vcl.StdCtrls;

type
  TfrmReplaceGroup = class(TForm)
    Label1: TLabel;
    Edit1: TEdit;
    Label2: TLabel;
    Edit2: TEdit;
    CheckBox1: TCheckBox;
    Button1: TButton;
    Button2: TButton;
    procedure Button1Click(Sender: TObject);
  private
    { Private declarations }
  public
    { Public declarations }
  end;

var
  frmReplaceGroup: TfrmReplaceGroup;

function ShowReplaceGroup(Sender: TComponent; const Title: string; var FindStr, NewStr: string; var Flag: Integer): Boolean;

implementation

{$R *.dfm}

function ShowReplaceGroup(Sender: TComponent; const Title: string; var FindStr, NewStr: string; var Flag: Integer): Boolean;
var
  F: TfrmReplaceGroup;
begin
  F := TfrmReplaceGroup.Create(Sender);
  try
    F.Caption := Title;
    F.CheckBox1.Enabled := Flag = 0;
    Result := F.ShowModal = mrOk;
    if Result then begin
      FindStr := Trim(F.Edit1.Text);
      NewStr := Trim(F.Edit2.Text);
      Flag := Ord(F.CheckBox1.Checked);
    end;
  finally
    F.Free;
  end;
end;

procedure TfrmReplaceGroup.Button1Click(Sender: TObject);
begin
//  if Trim(Edit1.Text) = '' then begin
//    ShowMessage('请输入要查找的内容');
//    Exit;
//  end;
  ModalResult := mrOk;
end;

end.
