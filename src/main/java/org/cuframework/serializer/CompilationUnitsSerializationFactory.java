// Compilation Units Framework: a very generic & powerful data driven programming framework.
// Copyright (c) 2019 Sidharth Yadav, sidharth_08@yahoo.com
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

package org.cuframework.serializer;

/**
 * Factory class to return serializer implementations.
 * @author Sidharth Yadav
 *
 */
public final class CompilationUnitsSerializationFactory {
    public static enum SerializerType {
        JSON("json"),
        MAP("map"),
        OBJECT("object"),
        SOURCE("source");

        private String type = "";
        private SerializerType(String type) {
            this.type = type;
        }
        public String getAsString() {
            return type;
        }
        public static SerializerType fromString(String type) {
            if (type == null)
                return null;
            switch(type.toLowerCase()) {
                case "json": return JSON;
                case "map": return MAP;
                case "object": return OBJECT;
                case "source": return SOURCE;
            }
            return null;
        }
    };

    private CompilationUnitsSerializationFactory() {
    }

    public static ICompilationUnitSerializer getGroupSerializer(SerializerType type) {
        type = type == null? SerializerType.JSON: type;  //default to JSON type if no type specified
        switch(type) {
            case SOURCE : return new SourceSerializerForGroupCU();
            case MAP :
            case OBJECT : return new ObjectSerializerForGroupCU();
            case JSON :
            default : return new JSONSerializerForGroupCU();
        }
    }
}
