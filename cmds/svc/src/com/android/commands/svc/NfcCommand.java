/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.commands.svc;

import java.io.IOException;

public class NfcCommand extends Svc.Command {

    public NfcCommand() {
        super("nfc");
    }

    @Override
    public String shortHelp() {
        return "Control NFC functions";
    }

    @Override
    public String longHelp() {
        return shortHelp() + "\n"
                + "\n"
                + "usage: svc nfc [enable|disable]\n"
                + "         Turn NFC on or off.\n\n";
    }

    @Override
    public void run(String[] args) {
        if (args.length != 2 ||
                !("enable".equals(args[1]) || "disable".equals(args[1]))) {
            System.err.println(longHelp());
            return;
        }

        // The NFC shell interface supplies the correct caller attribution and avoids
        // depending on hidden APIs in the modular NFC framework.
        ProcessBuilder command = "enable".equals(args[1])
                ? new ProcessBuilder("/system/bin/cmd", "nfc", "enable-nfc")
                : new ProcessBuilder("/system/bin/cmd", "nfc", "disable-nfc", "[persist]");
        try {
            int result = command.inheritIO().start().waitFor();
            if (result != 0) {
                System.err.println("NFC command failed with exit code " + result);
            }
        } catch (IOException e) {
            System.err.println("Unable to execute NFC command: " + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted while waiting for NFC command");
        }
    }

}
