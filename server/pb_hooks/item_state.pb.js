// Route de lecture : état calculé d'un ou plusieurs objets
routerAdd("GET", "/api/mutualisons/item-state", (e) => {
    const ids = (e.requestInfo().query.ids || "").split(",").map((s) => s.trim()).filter((s) => s !== "");
    if (ids.length === 0) throw new BadRequestError("Aucun objet demandé.");
    if (ids.length > 200) throw new BadRequestError("Trop d'objets en une seule requête.");

    const maintenant = new Date();
    const pbDate = (d) => d.toISOString().replace("T", " ");
    const result = {};

    ids.forEach((id) => {
        let item;
        try {
            item = $app.findRecordById("items", id);
        } catch (err) {
            return; // objet inconnu ou invisible : ignoré
        }

        const manuel = item.getString("status");
        // Vérifie que l'utilisateur peut voir cet objet (règle de la collection)
        const visibles = $app.findRecordsByFilter(
            "items", "id = {:i}", "", 1, 0, { i: id }
        );
        if (visibles.length === 0) return;

        const prets = $app.findRecordsByFilter(
            "loans",
            "item = {:i} && returned_at = ''",
            "+start_date", 50, 0,
            { i: id }
        );

        let etat = "available";
        let pret = null;
        for (const p of prets) {
            const debut = new Date(p.getString("start_date").replace(" ", "T"));
            if (debut <= maintenant) {
                etat = "lent";
                pret = p;
                break;
            }
            if (etat === "available") {
                etat = "reserved";
                pret = p;
            }
        }

        if (manuel === "repair" || manuel === "hidden") {
            etat = manuel;
        }

        result[id] = {
            state: etat,
            loan: pret ? {
                id: pret.id,
                borrower: pret.getString("borrower"),
                lender: pret.getString("lender"),
                start_date: pret.getString("start_date"),
                due_date: pret.getString("due_date")
            } : null
        };
    });

    return e.json(200, result);
}, $apis.requireAuth());