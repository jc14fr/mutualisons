routerAdd("POST", "/api/mutualisons/leave", (e) => {
    const circleId = (e.requestInfo().body.circle || "").trim();
    if (!circleId) throw new BadRequestError("Cercle manquant.");

    const circle = $app.findRecordById("circles", circleId);
    const members = circle.getStringSlice("members");
    if (!members.includes(e.auth.id)) {
        throw new BadRequestError("Vous n'êtes pas membre de ce cercle.");
    }

    const enCours = $app.findRecordsByFilter(
        "loans",
        "returned_at = '' && (lender = {:u} || borrower = {:u}) && item.circle = {:c}",
        "", 1, 0, { u: e.auth.id, c: circleId }
    );
    if (enCours.length > 0) {
        throw new BadRequestError("Un prêt est en cours dans ce cercle. Rendez-le avant de partir.");
    }

    const mesObjets = $app.findRecordsByFilter(
        "items", "circle = {:c} && owner = {:u}", "", 500, 0,
        { c: circleId, u: e.auth.id }
    );
    mesObjets.forEach((item) => {
        item.set("circle", "");
        $app.save(item);
    });

    const restants = members.filter((id) => id !== e.auth.id);
    if (restants.length === 0) {
        $app.delete(circle);
    } else {
        circle.set("members", restants);
        $app.save(circle);
    }

    return e.json(200, { ok: true });
}, $apis.requireAuth());

onRecordDeleteRequest((e) => {
    const uid = e.record.id;

    const enCours = $app.findRecordsByFilter(
        "loans",
        "returned_at = '' && (lender = {:u} || borrower = {:u})",
        "", 1, 0, { u: uid }
    );
    if (enCours.length > 0) {
        throw new BadRequestError("Des prêts sont en cours. Clôturez-les avant de supprimer votre compte.");
    }

    const mesObjets = $app.findRecordsByFilter("items", "owner = {:u}", "", 1000, 0, { u: uid });
    mesObjets.forEach((item) => $app.delete(item));

    const mesCercles = $app.findRecordsByFilter("circles", "members.id ?= {:u}", "", 500, 0, { u: uid });
    mesCercles.forEach((c) => {
        const restants = c.getStringSlice("members").filter((id) => id !== uid);
        if (restants.length === 0) {
            $app.delete(c);
        } else {
            c.set("members", restants);
            $app.save(c);
        }
    });

    const mesInvit = $app.findRecordsByFilter("invitations", "created_by = {:u}", "", 500, 0, { u: uid });
    mesInvit.forEach((i) => $app.delete(i));

    e.next();
}, "users");