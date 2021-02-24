/*
 * Copyright © 2018 Copyright (c) 2018 Yoyodyne, Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.sina.cli.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.sina.cli.api.SinaCliCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SinaCliCommandsImpl implements SinaCliCommands {

    private static final Logger LOG = LoggerFactory.getLogger(SinaCliCommandsImpl.class);
    private final DataBroker dataBroker;

    public SinaCliCommandsImpl(final DataBroker db) {
        this.dataBroker = db;
        LOG.info("SinaCliCommandImpl initialized");
    }

    @Override
    public Object testCommand(Object testArgument) {
        return "This is a test implementation of test-command";
    }
}
