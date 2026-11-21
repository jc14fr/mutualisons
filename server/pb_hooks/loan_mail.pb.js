onRecordAfterCreateSuccess((e) => {
    e.next();

    try {
        const owner = $app.findRecordById("users", e.record.get("owner"));
        const requester = $app.findRecordById("users", e.record.get("requester"));
        const item = $app.findRecordById("items", e.record.get("item"));

        const parseDate = (v) => new Date(String(v).replace(" ", "T"));
        const fr = (v) => {
            const d = parseDate(v);
            return d.toLocaleDateString("fr-FR", { timeZone: "Europe/Paris" });
        };

        const debut = fr(e.record.get("start_date"));
        const fin = fr(e.record.get("due_date"));

        const message = new MailerMessage({
            from: { address: "jeancyril@jc14fr.ovh", name: "Mutualisons" },
            to: [{ address: owner.get("email") }],
            subject: `Demande de prêt : « ${item.get("title")} » du ${debut} au ${fin}`,
            html: `<p>Bonjour ${owner.get("name")},</p>
                   <p><b>${requester.get("name")}</b> demande à emprunter :</p>
                   <ul>
                     <li><b>Objet :</b> ${item.get("title")}</li>
                     <li><b>Période :</b> du ${debut} au ${fin}</li>
                   </ul>
                   <p>Répondez dans l'onglet <b>Demandes</b> de l'application Mutualisons.</p>`,
        });
        $app.newMailClient().send(message);
    } catch (err) {
        console.log("loan_mail: envoi impossible : " + err);
    }
}, "loan_requests");