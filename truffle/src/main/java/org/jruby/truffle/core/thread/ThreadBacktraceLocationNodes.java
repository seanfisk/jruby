/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.core.thread;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.source.SourceSection;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.truffle.builtins.CoreClass;
import org.jruby.truffle.builtins.CoreMethod;
import org.jruby.truffle.builtins.UnaryCoreMethodNode;
import org.jruby.truffle.core.rope.RopeOperations;
import org.jruby.truffle.core.string.StringOperations;
import org.jruby.truffle.language.backtrace.Activation;

@CoreClass("Thread::Backtrace::Location")
public class ThreadBacktraceLocationNodes {

    @CoreMethod(names = { "absolute_path", "path" })
    // TODO (eregon, 8 July 2015): these two methods are slightly different (path can be relative if it is the main script)
    public abstract static class AbsolutePathNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject absolutePath(DynamicObject threadBacktraceLocation) {
            final Activation activation = ThreadBacktraceLocationLayoutImpl.INSTANCE.getActivation(threadBacktraceLocation);

            if (activation.getCallNode() == null) {
                return coreStrings().BACKTRACE_OMITTED_LIMIT.createInstance();
            }

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            if (sourceSection.getSource() == null) {
                return createString(StringOperations.encodeRope(String.format("%s:%d", sourceSection.getSource().getName(), sourceSection.getStartLine()), UTF8Encoding.INSTANCE));
            }

            // TODO CS 30-Apr-15: not absolute - not sure how to solve that

            final String path = sourceSection.getSource().getName();

            if (path == null) {
                return coreStrings().UNKNOWN.createInstance();
            } else {
                return createString(getContext().getRopeTable().getRope(path));
            }
        }

    }

    @CoreMethod(names = "lineno")
    public abstract static class LinenoNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public int lineno(DynamicObject threadBacktraceLocation) {
            final Activation activation = ThreadBacktraceLocationLayoutImpl.INSTANCE.getActivation(threadBacktraceLocation);

            final SourceSection sourceSection = activation.getCallNode().getEncapsulatingSourceSection();

            return sourceSection.getStartLine();
        }

    }

    @CoreMethod(names = {"to_s", "inspect"})
    public abstract static class ToSNode extends UnaryCoreMethodNode {

        @TruffleBoundary
        @Specialization
        public DynamicObject toS(DynamicObject threadBacktraceLocation) {
            final Activation activation = ThreadBacktraceLocationLayoutImpl.INSTANCE
                    .getActivation(threadBacktraceLocation);

            if (activation.getCallNode() == null) {
                return coreStrings().BACKTRACE_OMITTED_LIMIT.createInstance();
            }

            final Node callNode = activation.getCallNode();

            final RootNode rootNode = callNode.getRootNode();

            final SourceSection sourceSection = callNode.getEncapsulatingSourceSection();

            if (sourceSection.getSource() == null) {
                return createString(StringOperations.encodeRope(String.format("%s:%d", sourceSection.getSource().getName(), sourceSection.getStartLine()), UTF8Encoding.INSTANCE));
            }

            return createString(RopeOperations.format(getContext(),
                    sourceSection.getSource().getName(),
                    ":",
                    sourceSection.getStartLine(),
                    ":in `",
                    rootNode.getName(),
                    "'"));
        }

    }

}
