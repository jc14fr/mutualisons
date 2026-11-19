onRecordCreateRequest((e) => {
    if (!e.hasSuperuserAuth()) {
        e.record.set("approved", false);
    }
    e.next();
}, "users");

onRecordUpdateRequest((e) => {
    const avant = e.record.original().getBool("approved");
    const apres = e.record.getBool("approved");
    if (avant !== apres && !e.hasSuperuserAuth()) {
        throw new BadRequestError("Seul l'administrateur peut valider un compte.");
    }
    e.next();
}, "users");

onRecordAuthWithPasswordRequest((e) => {
    if (!e.record.getBool("approved")) {
        throw new BadRequestError("Votre compte est en attente de validation par l'administrateur.");
    }
    e.next();
}, "users");

onRecordAuthRefreshRequest((e) => {
    if (!e.record.getBool("approved")) {
        throw new BadRequestError("Votre compte n'est plus validé.");
    }
    e.next();
}, "users");