unit uFrmWait;

interface

uses
  Winapi.Windows, Winapi.Messages, System.SysUtils, System.Variants, System.Classes, Vcl.Graphics,
  Vcl.Controls, Vcl.Forms, Vcl.Dialogs, Vcl.StdCtrls, Vcl.WinXCtrls,
  Vcl.ExtCtrls;

type
  TForm2 = class(TForm)
    Panel1: TPanel;
    ActivityIndicator1: TActivityIndicator;
    Label1: TLabel;
    Button1: TButton;
    Timer1: TTimer;
    procedure Button1Click(Sender: TObject);
    procedure FormClose(Sender: TObject; var Action: TCloseAction);
    procedure Timer1Timer(Sender: TObject);
  private
    { Private declarations }
  public
    { Public declarations }
  end;

var
  Form2: TForm2;

procedure ShowWaitDlg();
procedure HideWaitDlg();

implementation

{$R *.dfm}

uses
  uFrmMain;

var
  fWait: TForm2;

procedure ShowWaitDlg();
begin
  if Assigned(fWait) then
    Exit;
  fWait := TForm2.Create(Application);
  fWait.ShowModal;
end;

procedure HideWaitDlg();
begin
  if Assigned(fWait) then
    fWait.Timer1.Enabled := True;
end;

procedure TForm2.Button1Click(Sender: TObject);
begin
  if Form1.Button1.Tag <> 0 then begin
    Timer1.Enabled := True;
    Form1.Button1Click(Button1);
  end;
end;

procedure TForm2.FormClose(Sender: TObject; var Action: TCloseAction);
begin
  fWait := nil;
  Action := caFree;
end;

procedure TForm2.Timer1Timer(Sender: TObject);
begin
  if Form1.Button1.Tag = 0 then begin
    ModalResult := mrCancel;
  end;
end;

end.
