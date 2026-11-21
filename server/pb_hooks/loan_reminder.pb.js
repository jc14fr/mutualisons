cronAdd("rappels_retour", "0 9 * * *", () => {
    const maintenant = new Date();
    const dans24h = new Date(maintenant.getTime() + 24 * 3600 * 1000);
    const dans48h = new Date(maintenant.getTime() + 48 * 3600 * 1000);

    const pbDate = (d) => d.toISOString().replace("T", " ");

    // Prêts en cours dont le retour tombe dans les prochaines 24 à 48 h, sans rappel envoyé
    const prets = $app.findRecordsByFilter(
        "loans",
        "returned_at = '' && reminder_sent = false && due_date > {:a} && due_date <= {:b}",
        "", 200, 0,
        { a: pbDate(dans24h), b: pbDate(dans48h) }
    );

    prets.forEach((loan) => {
        try {
            const borrower = $app.findRecordById("users", loan.get("borrower"));
            const owner = $app.findRecordById("users", loan.get("lender"));
            const item = $app.findRecordById("items", loan.get("item"));

            const fr = (v) => {
                const d = new Date(String(v).replace(" ", "T"));
                const p = new Intl.DateTimeFormat("fr-FR", {
                    timeZone: "Europe/Paris", day: "2-digit", month: "2-digit", year: "numeric"
                }).formatToParts(d);
                const get = (t) => p.find((x) => x.type === t).value;
                return `${get("day")}/${get("month")}/${get("year")}`;
            };

            const message = new MailerMessage({
                from: { address: "jeancyril@jc14fr.ovh", name: "Mutualisons" },
                to: [{ address: borrower.get("email") }],
                subject: `Rappel : « ${item.get("title")} » à rendre le ${fr(loan.get("due_date"))}`,
                html: `<p>Bonjour ${borrower.get("name")},</p>
                       <p>Petit rappel : vous avez emprunté <b>« ${item.get("title")} »</b>
                       à <b>${owner.get("name")}</b>, à rendre le <b>${fr(loan.get("due_date"))}</b>.</p>
                       <p>Merci de le rapporter à temps. Bonne journée !</p>`,
            });
            $app.newMailClient().send(message);

            loan.set("reminder_sent", true);
            $app.save(loan);
        } catch (err) {
            console.log("loan_reminder: échec pour un prêt : " + err);
        }
    });
});