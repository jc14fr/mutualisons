onRecordAfterCreateSuccess((e) => {
    const token = $security.randomString(40);
    const expires = new Date(Date.now() + 48 * 3600 * 1000);

    e.record.set("approval_token", token);
    e.record.set("approval_expires", expires.toISOString());
    $app.save(e.record);

    const link = `${$os.getenv("PUBLIC_URL")}/api/mutualisons/approve?token=${token}`;
    const message = new MailerMessage({
        from: { address: "jeancyril@jc14fr.ovh", name: "Mutualisons" },
        to: [{ address: $os.getenv("ADMIN_NOTIFY_EMAIL") }],
        subject: "Nouvelle demande d'inscription",
        html: `<p>Demande de ${e.record.get("name")} (${e.record.get("email")}).</p>
               <p><a href="${link}">Examiner la demande</a> (valable 48 h)</p>`,
    });
    $app.newMailClient().send(message);
}, "users");

routerAdd("GET", "/api/mutualisons/approve", (e) => {
    const token = e.requestInfo().query.token || "";
    if (!token) return e.html(400, "<p>Lien invalide.</p>");

    let user;
    try {
        user = $app.findFirstRecordByData("users", "approval_token", token);
    } catch (err) {
        return e.html(400, "<p>Lien invalide ou déjà utilisé.</p>");
    }
    if (!user.get("approval_expires") || new Date(user.get("approval_expires")) < new Date()) {
        return e.html(400, "<p>Lien expiré.</p>");
    }
    return e.html(200, `
        <p>Valider le compte de <b>${user.get("name")}</b> (${user.get("email")}) ?</p>
        <form method="POST" action="/api/mutualisons/approve">
          <input type="hidden" name="token" value="${token}">
          <button type="submit">Valider</button>
        </form>`);
});

routerAdd("POST", "/api/mutualisons/approve", (e) => {
    const token = e.requestInfo().body.token || "";
    if (!token) return e.html(400, "<p>Lien invalide.</p>");

    let user;
    try {
        user = $app.findFirstRecordByData("users", "approval_token", token);
    } catch (err) {
        return e.html(400, "<p>Lien invalide ou déjà utilisé.</p>");
    }
    if (!user.get("approval_expires") || new Date(user.get("approval_expires")) < new Date()) {
        return e.html(400, "<p>Lien expiré.</p>");
    }
    user.set("approved", true);
    user.set("approval_token", "");
    user.set("approval_expires", "");
    $app.save(user);
    return e.html(200, "<p>Compte validé.</p>");
});