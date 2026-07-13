package dev.vilquer.petcarescheduler.infra.adapter.output.mail

/**
 * Layout compartilhado dos e-mails transacionais do RotinaPet.
 *
 * Todo e-mail enviado pelo sistema passa por [render], que aplica a identidade
 * visual do app (paleta `--q-*` do frontend, wordmark e símbolo) sobre uma
 * estrutura de tabelas compatível com Gmail, Outlook e clientes mobile:
 * largura fluida com teto de 600px, botões "bulletproof" em tabela e cores
 * fixadas no modo claro (`color-scheme: light`), já que dark mode em e-mail
 * é reinterpretado de forma imprevisível por cada cliente.
 *
 * O símbolo da marca é servido como PNG pelo próprio frontend em
 * `<baseUrl>/email/simbolo.png` — SVG não é suportado por Gmail/Outlook.
 * O wordmark é texto estilizado, então o cabeçalho continua legível mesmo
 * com imagens bloqueadas pelo cliente de e-mail.
 */
object RotinaPetEmail {

    // Paleta espelhada dos tokens --q-* (variante clara) do frontend.
    const val GREEN = "#265949"
    const val GREEN_DARK = "#1B4033"
    const val IPE = "#DFA32E"
    const val INK = "#20261F"
    const val TEXT_2 = "#5A6253"
    const val TEXT_3 = "#878E7C"
    const val BG = "#F6F2E8"
    const val SURFACE_2 = "#FBF8F0"
    const val BORDER = "#E7E1CE"
    const val ERROR = "#B3402F"
    const val ERROR_BG = "#F9E4E0"
    const val WARNING = "#9A6B10"
    const val WARNING_BG = "#F8EFD8"

    private const val FONT =
        "'Hanken Grotesk',-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif"
    private const val FONT_DISPLAY =
        "'Bricolage Grotesque','Hanken Grotesk','Segoe UI',Helvetica,Arial,sans-serif"

    fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    /** Título principal do card. */
    fun title(text: String, color: String = INK): String =
        """<h1 class="rp-title" style="margin:0 0 16px;font-family:$FONT_DISPLAY;font-size:23px;line-height:30px;font-weight:700;letter-spacing:-0.3px;color:$color;">$text</h1>"""

    /** Parágrafo de corpo; aceita HTML já escapado (ex.: <strong>). */
    fun paragraph(html: String): String =
        """<p style="margin:0 0 16px;font-family:$FONT;font-size:15px;line-height:24px;color:$TEXT_2;">$html</p>"""

    /** Selo/pílula para categorizar o e-mail (ex.: tipo do evento). */
    fun badge(text: String, fg: String, bg: String): String =
        """<span style="display:inline-block;margin:0 0 14px;padding:5px 14px;border-radius:999px;background-color:$bg;font-family:$FONT;font-size:12px;line-height:16px;font-weight:700;letter-spacing:0.6px;text-transform:uppercase;color:$fg;">$text</span>"""

    /** Cartão de detalhes com pares rótulo/valor, um por linha. */
    fun detailCard(rows: List<Pair<String, String>>): String {
        val cells = rows.joinToString("") { (label, value) ->
            """
            <tr>
              <td style="padding:10px 18px;">
                <p style="margin:0;font-family:$FONT;font-size:11px;line-height:16px;font-weight:700;letter-spacing:0.8px;text-transform:uppercase;color:$TEXT_3;">$label</p>
                <p style="margin:2px 0 0;font-family:$FONT;font-size:15px;line-height:22px;font-weight:600;color:$INK;">$value</p>
              </td>
            </tr>"""
        }
        return """
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:8px 0 20px;background-color:$SURFACE_2;border:1px solid $BORDER;border-radius:12px;">
          <tr><td style="font-size:0;line-height:0;height:8px;">&nbsp;</td></tr>
          $cells
          <tr><td style="font-size:0;line-height:0;height:8px;">&nbsp;</td></tr>
        </table>"""
    }

    /** Aviso destacado (tom de alerta ou perigo). */
    fun notice(html: String, fg: String, bg: String): String =
        """
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:4px 0 20px;">
          <tr>
            <td style="padding:14px 18px;background-color:$bg;border-radius:12px;font-family:$FONT;font-size:14px;line-height:22px;color:$fg;">$html</td>
          </tr>
        </table>"""

    /** Botão principal em tabela ("bulletproof"), centralizado. */
    fun ctaButton(label: String, url: String, bg: String = GREEN): String =
        """
        <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:26px auto 10px;">
          <tr>
            <td align="center" bgcolor="$bg" style="border-radius:999px;">
              <a href="$url" target="_blank" rel="noopener" style="display:inline-block;padding:14px 34px;font-family:$FONT;font-size:15px;line-height:20px;font-weight:700;color:#FFFFFF;text-decoration:none;border-radius:999px;">$label</a>
            </td>
          </tr>
        </table>"""

    /** Link bruto exibido sob o CTA, para clientes que bloqueiam botões. */
    fun fallbackLink(url: String): String =
        """
        <p style="margin:14px 0 0;font-family:$FONT;font-size:12px;line-height:18px;color:$TEXT_3;text-align:center;">
          Se o botão não funcionar, copie e cole este endereço no navegador:<br>
          <a href="$url" target="_blank" rel="noopener" style="color:$GREEN;word-break:break-all;">$url</a>
        </p>"""

    fun divider(): String =
        """<table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin:22px 0;"><tr><td style="border-top:1px solid $BORDER;font-size:0;line-height:0;">&nbsp;</td></tr></table>"""

    /** Nota discreta ao final do card (ex.: "se não foi você, ignore"). */
    fun mutedNote(html: String): String =
        """<p style="margin:0;font-family:$FONT;font-size:13px;line-height:20px;color:$TEXT_3;">$html</p>"""

    /**
     * Monta o documento completo: fundo da marca, cabeçalho com símbolo +
     * wordmark, card branco com [contentHtml] e rodapé com o motivo do envio.
     *
     * @param preheader resumo invisível exibido pelos clientes na listagem.
     * @param footerReason por que o destinatário está recebendo este e-mail.
     */
    fun render(
        docTitle: String,
        preheader: String,
        contentHtml: String,
        footerReason: String,
        baseUrl: String,
    ): String {
        val base = baseUrl.trimEnd('/')
        val host = base.removePrefix("https://").removePrefix("http://")
        return """
<!DOCTYPE html>
<html lang="pt-BR" xmlns="http://www.w3.org/1999/xhtml">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta name="color-scheme" content="light">
  <meta name="supported-color-schemes" content="light">
  <meta name="x-apple-disable-message-reformatting">
  <title>$docTitle</title>
  <style>
    body { margin:0; padding:0; -webkit-text-size-adjust:100%; }
    @media only screen and (max-width: 620px) {
      .rp-container { padding: 20px 10px 28px !important; }
      .rp-card { padding: 28px 22px !important; }
      .rp-title { font-size: 21px !important; line-height: 28px !important; }
    }
  </style>
</head>
<body style="margin:0;padding:0;background-color:$BG;">
  <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;">$preheader&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;&nbsp;&zwnj;</div>
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" bgcolor="$BG" style="background-color:$BG;">
    <tr>
      <td align="center" class="rp-container" style="padding:32px 16px 40px;">
        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="max-width:600px;">
          <tr>
            <td align="center" style="padding:0 0 22px;">
              <a href="$base" target="_blank" rel="noopener" style="text-decoration:none;">
                <img src="$base/email/simbolo.png" width="42" height="42" alt="" style="display:inline-block;vertical-align:middle;border:0;margin-right:10px;">
                <span style="font-family:$FONT_DISPLAY;font-size:27px;font-weight:700;letter-spacing:-1px;vertical-align:middle;"><span style="color:$INK;">rotina</span><span style="color:$GREEN;">pet</span><span style="color:$IPE;">.</span></span>
              </a>
            </td>
          </tr>
          <tr>
            <td class="rp-card" bgcolor="#FFFFFF" style="background-color:#FFFFFF;border:1px solid $BORDER;border-radius:16px;padding:36px 40px;">
              $contentHtml
            </td>
          </tr>
          <tr>
            <td align="center" style="padding:26px 20px 0;">
              <p style="margin:0 0 8px;font-family:$FONT;font-size:12px;line-height:19px;color:$TEXT_3;">$footerReason</p>
              <p style="margin:0 0 14px;font-family:$FONT;font-size:12px;line-height:19px;color:$TEXT_3;">
                <a href="$base" target="_blank" rel="noopener" style="color:$GREEN;text-decoration:none;font-weight:600;">$host</a>
              </p>
              <p style="margin:0;font-family:$FONT_DISPLAY;font-size:12px;line-height:19px;color:$TEXT_3;font-style:italic;">A vida de um pet é feita de rotina. A rotina merece um lugar bonito.</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>"""
    }
}
